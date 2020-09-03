/*
 * Copyright 2013 - 2020 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TeltonikaProtocolDecoder extends BaseProtocolDecoder {

    private static final int IMAGE_PACKET_MAX = 2048;

    private final boolean connectionless;
    private boolean extended;
    private final Map<Long, ByteBuf> photos = new HashMap<>();

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public TeltonikaProtocolDecoder(Protocol protocol, boolean connectionless) {
        super(protocol);
        this.connectionless = connectionless;
        this.extended = Context.getConfig().getBoolean(getProtocolName() + ".extended");
    }

    private void parseIdentification(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int length = buf.readUnsignedShort();
        String imei = buf.toString(buf.readerIndex(), length, StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (channel != null) {
            ByteBuf response = Unpooled.buffer(1);
            if (deviceSession != null) {
                response.writeByte(1);
            } else {
                response.writeByte(0);
            }
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    public static final int CODEC_GH3000 = 0x07;
    public static final int CODEC_8 = 0x08;
    public static final int CODEC_8_EXT = 0x8E;
    public static final int CODEC_12 = 0x0C;
    public static final int CODEC_13 = 0x0D;
    public static final int CODEC_16 = 0x10;

    private void sendImageRequest(Channel channel, SocketAddress remoteAddress, long id, int offset, int size) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeInt(0);
            response.writeShort(0);
            response.writeShort(19); // length
            response.writeByte(CODEC_12);
            response.writeByte(1); // nod
            response.writeByte(0x0D); // camera
            response.writeInt(11); // payload length
            response.writeByte(2); // command
            response.writeInt((int) id);
            response.writeInt(offset);
            response.writeShort(size);
            response.writeByte(1); // nod
            response.writeShort(0);
            response.writeShort(Checksum.crc16(
                    Checksum.CRC16_IBM, response.nioBuffer(8, response.readableBytes() - 10)));
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private boolean isPrintable(ByteBuf buf, int length) {
        boolean printable = true;
        for (int i = 0; i < length; i++) {
            byte b = buf.getByte(buf.readerIndex() + i);
            if (b < 32 && b != '\r' && b != '\n') {
                printable = false;
                break;
            }
        }
        return printable;
    }

    private void decodeSerial(Channel channel, SocketAddress remoteAddress, Position position, ByteBuf buf) {

        getLastLocation(position, null);

        int type = buf.readUnsignedByte();
        if (type == 0x0D) {

            buf.readInt(); // length
            int subtype = buf.readUnsignedByte();
            if (subtype == 0x01) {

                long photoId = buf.readUnsignedInt();
                ByteBuf photo = Unpooled.buffer(buf.readInt());
                photos.put(photoId, photo);
                sendImageRequest(
                        channel, remoteAddress, photoId,
                        0, Math.min(IMAGE_PACKET_MAX, photo.capacity()));

            } else if (subtype == 0x02) {

                long photoId = buf.readUnsignedInt();
                buf.readInt(); // offset
                ByteBuf photo = photos.get(photoId);
                photo.writeBytes(buf, buf.readUnsignedShort());
                if (photo.writableBytes() > 0) {
                    sendImageRequest(
                            channel, remoteAddress, photoId,
                            photo.writerIndex(), Math.min(IMAGE_PACKET_MAX, photo.writableBytes()));
                } else {
                    String uniqueId = Context.getIdentityManager().getById(position.getDeviceId()).getUniqueId();
                    photos.remove(photoId);
                    try {
                        position.set(Position.KEY_IMAGE, Context.getMediaManager().writeFile(uniqueId, photo, "jpg"));
                    } finally {
                        photo.release();
                    }
                }

            }

        } else {

            position.set(Position.KEY_TYPE, type);

            int length = buf.readInt();
            if (isPrintable(buf, length)) {
                String data = buf.readSlice(length).toString(StandardCharsets.US_ASCII).trim();
                if (data.startsWith("UUUUww") && data.endsWith("SSS")) {
                    String[] values = data.substring(6, data.length() - 4).split(";");
                    for (int i = 0; i < 8; i++) {
                        position.set("axle" + (i + 1), Double.parseDouble(values[i]));
                    }
                    position.set("loadTruck", Double.parseDouble(values[8]));
                    position.set("loadTrailer", Double.parseDouble(values[9]));
                    position.set("totalTruck", Double.parseDouble(values[10]));
                    position.set("totalTrailer", Double.parseDouble(values[11]));
                } else {
                    position.set(Position.KEY_RESULT, data);
                }
            } else {
                position.set(Position.KEY_RESULT, ByteBufUtil.hexDump(buf.readSlice(length)));
            }
        }
    }

    private long readValue(ByteBuf buf, int length, boolean signed) {
        switch (length) {
            case 1:
                return signed ? buf.readByte() : buf.readUnsignedByte();
            case 2:
                return signed ? buf.readShort() : buf.readUnsignedShort();
            case 4:
                return signed ? buf.readInt() : buf.readUnsignedInt();
            default:
                return buf.readLong();
        }
    }

    private void decodeOtherParameter(Position position, int id, ByteBuf buf, int length) {
        switch (id) {
            case 1:
            case 2:
            case 3:
                position.set("DIN" + id, readValue(buf, length, false));
                break;
            case 4:
            case 5:
                position.set("Pulse Counter DIN" + (id - 3), readValue(buf, length, false));
                break;
            case 6:
                position.set("AIN" + 2, readValue(buf, length, false) * 0.001);
                break;
            case 7:
                position.set("Records In Flash", readValue(buf, length, false));
                break;
            case 8:
                position.set("Authorized iButton", String.format("%X", readValue(buf, length, false)));
                break;
            case 9:
                position.set("AIN" + 1, readValue(buf, length, false) * 0.001);
                break;
            case 10:
                position.set("SD Status", readValue(buf, length, false));
                break;
            case 11:
                position.set("ICCID1", readValue(buf, length, false));
                break;
            case 12:
                position.set("Fuel Used GNSS (l)", readValue(buf, length, false) * 0.001);
                break;
            case 13:
                position.set("Fuel Rate GNSS (l/100km)", readValue(buf, length, false) * 0.01);
                break;
            case 14:
                position.set("ICCID2", readValue(buf, length, false));
                break;
            case 15:
                position.set("Eco Score", readValue(buf, length, false) * 0.01);
                break;
            case 16:
                position.set("Total Odometer (m)", readValue(buf, length, false));
                break;
            case 17:
                position.set("Axis X (mG)", readValue(buf, length, true));
                break;
            case 18:
                position.set("Axis Y (mG)", readValue(buf, length, true));
                break;
            case 19:
                position.set("Axis Z (mG)", readValue(buf, length, true));
                break;
            case 20:
                position.set("BLE 2 Battery Voltage", readValue(buf, length, false));
                break;
            case 21:
                position.set(Position.KEY_RSSI, readValue(buf, length, false));
                break;
            case 22:
                position.set("BLE 3 Battery Voltage", readValue(buf, length, false));
                break;
            case 23:
                position.set("BLE 4 Battery Voltage", readValue(buf, length, false));
                break;
            case 24:
                readValue(buf, length, false); // speed
                break;
            case 25:
            case 26:
            case 27:
            case 28:
                position.set("BLE Temperature #" + (id - 24), readValue(buf, length, true) * 0.01);
                break;
            case 29:
                position.set("BLE 1 Battery Voltage", readValue(buf, length, false));
                break;
            case 31:
                position.set("Engine Load (%) (OBD)", readValue(buf, length, false));
                break;
            case 32:
                position.set("Coolant Temperature (C) (OBD)", readValue(buf, length, true));
                break;
            case 33:
                position.set("Short Fuel Trim (%) (OBD)", readValue(buf, length, true));
                break;
            case 34:
                position.set("Fuel Pressure (kPa) (OBD)", readValue(buf, length, false));
                break;
            case 35:
                position.set("Intake MAP (kPa) (OBD)", readValue(buf, length, false));
                break;
            case 36:
                position.set("Engine RPM (OBD)", readValue(buf, length, false));
                break;
            case 37:
                position.set("Vehicle Speed (km/h) (OBD)", readValue(buf, length, false));
                break;
            case 38:
                position.set("Timing Advance (deg) (OBD)", readValue(buf, length, true));
                break;
            case 39:
                position.set("Intake Air Temperature (C) (OBD)", readValue(buf, length, true));
                break;
            case 40:
                position.set("MAF (g/sec) (OBD)", readValue(buf, length, false) * 0.01);
                break;
            case 41:
                position.set("Throttle Position (%) (OBD)", readValue(buf, length, false));
                break;
            case 42:
                position.set("Runtime Since Engine Start (s) (OBD)", readValue(buf, length, false));
                break;
            case 43:
                position.set("Distance Traveled MIL On (km) (OBD)", readValue(buf, length, false));
                break;
            case 44:
                position.set("Relative Fuel Rail Pressure (kPa) (OBD)", readValue(buf, length, false) * 0.1);
                break;
            case 45:
                position.set("Direct Fuel Rail Pressure (kPa) (OBD)", readValue(buf, length, false) * 10);
                break;
            case 46:
                position.set("Commanded EGR (%) (OBD)", readValue(buf, length, false));
                break;
            case 47:
                position.set("EGR Error (%) (OBD)", readValue(buf, length, true));
                break;
            case 48:
                position.set("Fuel Level (%) (OBD)", readValue(buf, length, false));
                break;
            case 49:
                position.set("Distance Since Codes Clear (km) (OBD)", readValue(buf, length, false));
                break;
            case 50:
                position.set("Barometric Pressure (kPa) (OBD)", readValue(buf, length, false));
                break;
            case 51:
                position.set("Control Module Voltage (V) (OBD)", readValue(buf, length, false) * 0.001);
                break;
            case 52:
                position.set("Absolute Load Value (%) (OBD)", readValue(buf, length, false));
                break;
            case 53:
                position.set("Ambient Air Temperature (C) (OBD)", readValue(buf, length, true));
                break;
            case 54:
                position.set("Time Run With MIL On (min) (OBD)", readValue(buf, length, false));
                break;
            case 55:
                position.set("Time Since Codes Cleared (min) (OBD)", readValue(buf, length, false));
                break;
            case 56:
                position.set("Absolute Fuel Rail Pressure (kPa) (OBD)", readValue(buf, length, false) * 0.1);
                break;
            case 57:
                position.set("Hybrid battery pack life (%) (OBD)", readValue(buf, length, false));
                break;
            case 58:
                position.set("Engine Oil Temperature (C) (OBD)", readValue(buf, length, false));
                break;
            case 59:
                position.set("Fuel Injection Timing (deg) (OBD)", readValue(buf, length, true) * 0.01);
                break;
            case 60:
                position.set("Fuel Rate (l/100km) (OBD)", readValue(buf, length, false) * 0.01);
                break;
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
                position.set("Geofence zone " + (id - 60 + 5), readValue(buf, length, false));
                break;
            case 66:
                position.set("External voltage", readValue(buf, length, false) * 0.001);
                break;
            case 67:
                position.set("Battery voltage", readValue(buf, length, false) * 0.001);
                break;
            case 68:
                position.set("Battery Current", readValue(buf, length, false) * 0.001);
                break;
            case 69:
                position.set("GNSS Status", readValue(buf, length, false));
                break;
            case 70:
                position.set("Geofence zone 11", readValue(buf, length, false));
                break;
            case 71:
                position.set("Temperature ID 4", String.format("%X", readValue(buf, length, false)));
                break;
            case 72:
            case 73:
            case 74:
            case 75:
                position.set("Dallas temperature " + (id - 71), readValue(buf, length, true) * 0.1);
                break;
            case 76:
            case 77:
                position.set("Temperature ID " + (id - 75), String.format("%X", readValue(buf, length, false)));
                break;
            case 78:
                long driverUniqueId = readValue(buf, length, false);
                if (driverUniqueId != 0) {
                    position.set("iButton", String.format("%016X", driverUniqueId));
                }
                break;
            case 79:
                position.set("Temperature ID 3", String.format("%X", readValue(buf, length, false)));
                break;
            case 80:
                position.set("Data Mode", readValue(buf, length, false));
                break;
            case 81:
                position.set("Vehicle Speed (km/h) (CAN)", readValue(buf, length, false));
                break;
            case 82:
                position.set("Accelerator Pedal Position (%) (CAN)", readValue(buf, length, false));
                break;
            case 83:
                position.set("Fuel Consumed (l) (CAN)", readValue(buf, length, false) * 0.1);
                break;
            case 84:
                position.set("Fuel level (l) (CAN)", readValue(buf, length, false) * 0.1);
                break;
            case 85:
                position.set("Engine RPM (CAN)", readValue(buf, length, false));
                break;
            case 86:
                position.set("BLE 1 Humidity (%RH)", readValue(buf, length, false) * 0.1);
                break;
            case 87:
                position.set("Total Mileage (m) (CAN)", readValue(buf, length, false));
                break;
            case 88:
                position.set("Geofence zone 12", readValue(buf, length, false));
                break;
            case 89:
                position.set("Fuel Level (%) (CAN)", readValue(buf, length, false));
                break;
            case 90:
                position.set("Door Status (CAN)", readValue(buf, length, false));
                break;
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
                position.set("Geofence zone " + (id - 90 + 12), readValue(buf, length, false));
                break;
            case 100:
                position.set("Program Number (CAN)", readValue(buf, length, false));
                break;
            case 101:
                position.set("Module ID (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 102:
                position.set("Engine Worktime (min) (CAN)", readValue(buf, length, false));
                break;
            case 103:
                position.set("Engine Worktime (counted, min) (CAN)", readValue(buf, length, false));
                break;
            case 104:
                position.set("BLE 2 Humidity (%RH)", readValue(buf, length, false) * 0.1);
                break;
            case 105:
                position.set("Total Mileage (counted, m) (CAN)", readValue(buf, length, false));
                break;
            case 106:
                position.set("BLE 3 Humidity (%RH)", readValue(buf, length, false) * 0.1);
                break;
            case 107:
                position.set("Fuel Consumed (counted, ml) (CAN)", readValue(buf, length, false));
                break;
            case 108:
                position.set("BLE 4 Humidity (%RH)", readValue(buf, length, false) * 0.1);
                break;
            case 110:
                position.set("Fuel Rate (l/h) (CAN)", readValue(buf, length, false) * 0.1);
                break;
            case 111:
                position.set("AdBlue Level (%) (CAN)", readValue(buf, length, false));
                break;
            case 112:
                position.set("AdBlue Level (l) (CAN)", readValue(buf, length, false) * 0.1);
                break;
            case 113:
                if (length == 1) {
                    position.set("Battery level", readValue(buf, length, false));
                } else {
                    position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
                }
                break;
            case 114:
                position.set("Engine Load (%) (CAN)", readValue(buf, length, false));
                break;
            case 115:
                position.set("Engine Temperature (C) (CAN)", readValue(buf, length, true) * 0.1);
                break;
            case 116:
                position.set("Charger Connected", readValue(buf, length, false));
                break;
            case 117:
                position.set("Driving Direction", readValue(buf, length, false));
                break;
            case 118:
                position.set("Axle 1 Load (kg) (CAN)", readValue(buf, length, false));
                break;
            case 119:
                position.set("Axle 2 Load (kg) (CAN)", readValue(buf, length, false));
                break;
            case 120:
                position.set("Axle 3 Load (kg) (CAN)", readValue(buf, length, false));
                break;
            case 121:
                position.set("Axle 4 Load (kg) (CAN)", readValue(buf, length, false));
                break;
            case 122:
                position.set("Axle 5 Load (kg) (CAN)", readValue(buf, length, false));
                break;
            case 123:
                position.set("Control State Flags (CAN)", readValue(buf, length, false));
                break;
            case 124:
                position.set("Agricultural Machinery Flags (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 125:
                position.set("Harvesting Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 126:
                position.set("Area of Harvest (m2) (CAN)", readValue(buf, length, false));
                break;
            case 127:
                position.set("LVC of Harvest (m2/h) (CAN)", readValue(buf, length, false));
                break;
            case 128:
                position.set("Grain Mow Volume (kg) (CAN)", readValue(buf, length, false));
                break;
            case 129:
                position.set("Grain Moisture (%) (CAN)", readValue(buf, length, false));
                break;
            case 130:
                position.set("Harvesting Drum RPM (CAN)", readValue(buf, length, false));
                break;
            case 131:
                position.set("Gap Under Harvesting Drum (mm) (CAN)", readValue(buf, length, false));
                break;
            case 132:
                position.set("Security State Flags (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 133:
                position.set("Tacho Total Distance (m) (CAN)", readValue(buf, length, false));
                break;
            case 134:
                position.set("Trip Distance (m) (CAN)", readValue(buf, length, false));
                break;
            case 135:
                position.set("Tacho Vehicle Speed (km/h) (CAN)", readValue(buf, length, false));
                break;
            case 136:
                position.set("Tacho Driver Card Presence (CAN)", readValue(buf, length, false));
                break;
            case 137:
                position.set("Driver 1 States (CAN)", readValue(buf, length, false));
                break;
            case 138:
                position.set("Driver 2 States (CAN)", readValue(buf, length, false));
                break;
            case 139:
                position.set("Driver 1 Driving Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 140:
                position.set("Driver 2 Driving Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 141:
                position.set("Driver 1 Break Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 142:
                position.set("Driver 2 Break Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 143:
                position.set("Driver 1 Activity Duration (min) (CAN)", readValue(buf, length, false));
                break;
            case 144:
                position.set("Driver 2 Activity Duration (min) (CAN)", readValue(buf, length, false));
                break;
            case 145:
                position.set("Driver 1 Driving Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 146:
                position.set("Driver 2 Driving Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 147:
                position.set("Driver 1 ID High (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 148:
                position.set("Driver 1 ID Low (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 149:
                position.set("Driver 2 ID High (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 150:
                position.set("Driver 2 ID Low (CAN)", String.format("%X", readValue(buf, length, false)));
                break;
            case 151:
                position.set("Battery Temperature (C) (CAN)", readValue(buf, length, true) * 0.1);
                break;
            case 152:
                position.set("Battery Level (%) (CAN)", readValue(buf, length, false));
                break;
            case 153:
            case 154:
                position.set("Geofence zone " + (id - 152 + 21), readValue(buf, length, false));
                break;
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                position.set("Geofence zone " + (id - 154), readValue(buf, length, false));
                break;
            case 160:
                position.set("DTC Faults Count (CAN)", readValue(buf, length, false));
                break;
            case 161:
                position.set("Slope of Arm (deg) (CAN)", readValue(buf, length, true));
                break;
            case 162:
                position.set("Rotation of Arm (deg) (CAN)", readValue(buf, length, true));
                break;
            case 163:
                position.set("Eject of Arm (m) (CAN)", readValue(buf, length, false));
                break;
            case 164:
                position.set("Horizontal Distance Arm (m) (CAN)", readValue(buf, length, false));
                break;
            case 165:
                position.set("Height Arm Above Ground (m) (CAN)", readValue(buf, length, false));
                break;
            case 166:
                position.set("Drill RPM (CAN)", readValue(buf, length, false));
                break;
            case 167:
                position.set("Spread Salt (g/m2) (CAN)", readValue(buf, length, false));
                break;
            case 168:
                position.set("Battery Voltage (V) (CAN)", readValue(buf, length, false));
                break;
            case 169:
                position.set("Spread Fine Grained Salt (T) (CAN)", readValue(buf, length, false));
                break;
            case 170:
                position.set("Coarse Grained Salt (T) (CAN)", readValue(buf, length, false));
                break;
            case 171:
                position.set("Spread DiMix (T) (CAN)", readValue(buf, length, false));
                break;
            case 172:
                position.set("Spread Course Grained Calcium (m3) (CAN)", readValue(buf, length, false));
                break;
            case 173:
                position.set("Spread Calcium Chloride (m3) (CAN)", readValue(buf, length, false));
                break;
            case 174:
                position.set("Spread Sodium Chloride (m3) (CAN)", readValue(buf, length, false));
                break;
            case 175:
                position.set("AUTO GEOFENCE", readValue(buf, length, false));
                break;
            case 176:
                position.set("Spread Magnesium Chloride (m3) (CAN)", readValue(buf, length, false));
                break;
            case 177:
                position.set("Amount Of Spread Gravel (T) (CAN)", readValue(buf, length, false));
                break;
            case 178:
                position.set("Amount of Spread Sand (T) (CAN)", readValue(buf, length, false));
                break;
            case 179:
                position.set("DOUT" + 1, readValue(buf, length, false) == 1);
                break;
            case 180:
                position.set("DOUT" + 2, readValue(buf, length, false) == 1);
                break;
            case 181:
                position.set(Position.KEY_PDOP, readValue(buf, length, false) * 0.1);
                break;
            case 182:
                position.set(Position.KEY_HDOP, readValue(buf, length, false) * 0.1);
                break;
            case 183:
                position.set("Width Pouring Left (m) (CAN)", readValue(buf, length, false));
                break;
            case 184:
                position.set("Width Pouring Right (m) (CAN)", readValue(buf, length, false));
                break;
            case 185:
                position.set("Salt Spreader Working Hours (h) (CAN)", readValue(buf, length, false));
                break;
            case 186:
                position.set("Distance During Salting (km) (CAN)", readValue(buf, length, false));
                break;
            case 187:
                position.set("Load Weight (kg) (CAN)", readValue(buf, length, false));
                break;
            case 188:
                position.set("Retarder Load (%) (CAN)", readValue(buf, length, false));
                break;
            case 189:
                position.set("Cruise Time (min) (CAN)", readValue(buf, length, false));
                break;
            case 190:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
                position.set("Geofence zone " + (id - 189 + 23), readValue(buf, length, false));
                break;
            case 199:
                position.set("Trip Odometer", readValue(buf, length, false));
                break;
            case 200:
                position.set("Sleep Mode", readValue(buf, length, false));
                break;
            case 201:
                position.set("LLS 1 Fuel Level", readValue(buf, length, true));
                break;
            case 202:
                position.set("LLS 1 Temperature", readValue(buf, length, true));
                break;
            case 203:
                position.set("LLS 2 Fuel Level", readValue(buf, length, true));
                break;
            case 204:
                position.set("LLS 2 Temperature", readValue(buf, length, true));
                break;
            case 205:
                position.set("GSM Cell ID", readValue(buf, length, false));
                break;
            case 206:
                position.set("GSM Area Code", readValue(buf, length, false));
                break;
            case 207:
                position.set("RFID", String.format("%X", readValue(buf, length, false)));
                break;
            case 208:
            case 209:
                position.set("Geofence zone " + (id - 207 + 32), readValue(buf, length, false));
                break;
            case 210:
                position.set("LLS 3 Fuel Level", readValue(buf, length, true));
                break;
            case 211:
                position.set("LLS 3 Temperature", readValue(buf, length, true));
                break;
            case 212:
                position.set("LLS 4 Fuel Level", readValue(buf, length, true));
                break;
            case 213:
                position.set("LLS 4 Temperature", readValue(buf, length, true));
                break;
            case 214:
                position.set("LLS 5 Fuel Level", readValue(buf, length, true));
                break;
            case 215:
                position.set("LLS 5 Temperature", readValue(buf, length, true));
                break;
            case 216:
            case 217:
            case 218:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
                position.set("Geofence zone " + (id - 215 + 34), readValue(buf, length, false));
                break;
            case 232:
                position.set("CNG status (CAN)", readValue(buf, length, false));
                break;
            case 233:
                position.set("CNG used (kg) (CAN)", readValue(buf, length, false));
                break;
            case 234:
                position.set("CNG level (%) (CAN)", readValue(buf, length, false));
                break;
            case 235:
                position.set("Engine Oil Level (CAN)", readValue(buf, length, false));
                break;
            case 236:
                if (readValue(buf, length, false) == 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                }
                break;
            case 237:
                position.set("Network Type", readValue(buf, length, false));
                break;
            case 238:
                position.set("User ID", String.format("%X", readValue(buf, length, false)));
                break;
            case 239:
                position.set(Position.KEY_IGNITION, readValue(buf, length, false) == 1);
                break;
            case 240:
                position.set(Position.KEY_MOTION, readValue(buf, length, false) == 1);
                break;
            case 241:
                position.set(Position.KEY_OPERATOR, readValue(buf, length, false));
                break;
            case 242:
                position.set("MANDOWN", readValue(buf, length, false));
                break;
            case 243:
                position.set("GREEN DRIVING EVENT DURATION (ms)", readValue(buf, length, false));
                break;
            case 244:
                position.set("DIN2/AIN2 spec event", readValue(buf, length, false));
                break;
            case 246:
                position.set("TOWING", readValue(buf, length, false));
                break;
            case 247:
                position.set("CRASH DETECTION", readValue(buf, length, false));
                break;
            case 248:
                position.set("IMMOBILIZER", readValue(buf, length, false));
                break;
            case 249:
                position.set("JAMMING", readValue(buf, length, false));
                break;
            case 250:
                position.set("TRIP", readValue(buf, length, false));
                break;
            case 251:
                position.set("IDLING", readValue(buf, length, false));
                break;
            case 252:
                position.set("UNPLUG", readValue(buf, length, false));
                break;
            case 253:
                switch ((int) readValue(buf, length, false)) {
                    case 1:
                        position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                        break;
                    case 2:
                        position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                        break;
                    case 3:
                        position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
                        break;
                    default:
                        break;
                }
                break;
            case 254: 
                position.set("GREEN DRIVING VALUE", readValue(buf, length, false));
                break;
            case 255:
                position.set("OVER SPEEDING", readValue(buf, length, false));
                break;
            case 257:
                position.set("Crash trace data", readValue(buf, length, false));
                break;
            case 258:
                position.set("EcoMaximum", String.format("%X", readValue(buf, length, false)));
                break;
            case 259:
                position.set("EcoAverage", String.format("%X", readValue(buf, length, false)));
                break;
            case 260:
                position.set("EcoDuration (ms)", readValue(buf, length, false));
                break;
            case 263:
                position.set("BT Status", readValue(buf, length, false));
                break;
            case 269:
                position.set("Escort LLS Temperature #1", readValue(buf, length, true));
                break;
            case 270:
                position.set("Escort LLS Fuel Level #1", readValue(buf, length, false));
                break;
            case 271:
                position.set("Escort LLS Battery Voltage #1", readValue(buf, length, false) * 0.01);
                break;
            case 272:
                position.set("Escort LLS Temperature #2", readValue(buf, length, true));
                break;
            case 273:
                position.set("Escort LLS Fuel Level #2", readValue(buf, length, false));
                break;
            case 274:
                position.set("Escort LLS Battery Voltage #2", readValue(buf, length, false) * 0.01);
                break;
            case 275:
                position.set("Escort LLS Temperature #3", readValue(buf, length, true));
                break;
            case 276:
                position.set("Escort LLS Fuel Level #3", readValue(buf, length, false));
                break;
            case 277:
                position.set("Escort LLS Battery Voltage #3", readValue(buf, length, false) * 0.01);
                break;
            case 278:
                position.set("Escort LLS Temperature #4", readValue(buf, length, true));
                break;
            case 279:
                position.set("Escort LLS Fuel Level #4", readValue(buf, length, false));
                break;
            case 280:
                position.set("Escort LLS Battery Voltage #4", readValue(buf, length, false) * 0.01);
                break;
            case 282:
                position.set("DTC Faults code (CAN)", readValue(buf, length, false));
                break;
            case 283:
                position.set("Driving State", readValue(buf, length, false));
                break;
            case 284:
                position.set("Driving Records", readValue(buf, length, false));
                break;
            case 285:
                position.set("Blood alcohol content", readValue(buf, length, false));
                break;
            case 303:
                position.set("Instant Movement", readValue(buf, length, false));
                break;
            case 304:
                position.set("Vehicle Range on Battery (m) (CAN)", readValue(buf, length, false));
                break;
            case 306:
            case 307:
            case 308:
            case 309:
                position.set("BLE Fuel Frequency #" + (id - 305), readValue(buf, length, false));
            case 310:
                position.set("Movement Event", readValue(buf, length, false));
                break;
            case 314:
                position.set("Beacon", readValue(buf, length, false));
                break;
            case 315:
                position.set("Dead Man", readValue(buf, length, false));
                break;
            case 317:
                position.set("Crash event counter", readValue(buf, length, false));
                break;
            case 325:
                position.set("VIN (CAN)", readValue(buf, length, false));
                break;
            case 327:
                position.set("UL202-02 Sensor Fuel level (mm)", readValue(buf, length, true) * 0.1);
                break;
            case 328:
                position.set("UL202-02 Sensor Status", readValue(buf, length, false));
                break;
            case 330:
                position.set("Trip trace event", readValue(buf, length, false));
                break;
            case 335:
            case 336:
            case 337:
            case 338:
                position.set("BLE Luminosity (lx) " + (id - 334), readValue(buf, length, false));
                break;
            case 380:
                position.set("DOUT" + 3, readValue(buf, length, false) == 1);
                break;
            case 381:
                position.set("Ground sense", readValue(buf, length, false));
                break;
            case 389:
                position.set("OBD OEM Total Mileage (km)", readValue(buf, length, false));
                break;
            case 390:
                position.set("OBD OEM Fuel Level (l)", readValue(buf, length, false) * 0.1);
                break;
            case 391:
                position.set("Private mode", readValue(buf, length, false));
                break;
            case 483:
                position.set("UL202-02 Sensor Status", readValue(buf, length, false));
                break;
            case 517:
                position.set("SecurityStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            case 518:
                position.set("ControlStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            case 519:
                position.set("IndicatorStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            case 520:
                position.set("AgriculturalStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            case 521:
                position.set("UtilityStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            case 522:
                position.set("CisternStateFlags_P4", String.format("%X", readValue(buf, length, false)));
                break;
            default:
                position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
                break;
        }
    }

    private void decodeGh3000Parameter(Position position, int id, ByteBuf buf, int length) {
        switch (id) {
            case 1:
                position.set(Position.KEY_BATTERY_LEVEL, readValue(buf, length, false));
                break;
            case 2:
                position.set("usbConnected", readValue(buf, length, false) == 1);
                break;
            case 5:
                position.set("uptime", readValue(buf, length, false));
                break;
            case 20:
                position.set(Position.KEY_HDOP, readValue(buf, length, false) * 0.1);
                break;
            case 21:
                position.set(Position.KEY_VDOP, readValue(buf, length, false) * 0.1);
                break;
            case 22:
                position.set(Position.KEY_PDOP, readValue(buf, length, false) * 0.1);
                break;
            case 67:
                position.set(Position.KEY_BATTERY, readValue(buf, length, false) * 0.001);
                break;
            case 221:
                position.set("button", readValue(buf, length, false));
                break;
            case 222:
                if (readValue(buf, length, false) == 1) {
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                }
                break;
            case 240:
                position.set(Position.KEY_MOTION, readValue(buf, length, false) == 1);
                break;
            case 244:
                position.set(Position.KEY_ROAMING, readValue(buf, length, false) == 1);
                break;
            default:
                position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
                break;
        }
    }

    private void decodeParameter(Position position, int id, ByteBuf buf, int length, int codec) {
        if (codec == CODEC_GH3000) {
            decodeGh3000Parameter(position, id, buf, length);
        } else {
            decodeOtherParameter(position, id, buf, length);
        }
    }

    private void decodeNetwork(Position position) {
        long cid = position.getLong(Position.PREFIX_IO + 205);
        int lac = position.getInteger(Position.PREFIX_IO + 206);
        if (cid != 0 && lac != 0) {
            CellTower cellTower = CellTower.fromLacCid(lac, cid);
            long operator = position.getInteger(Position.KEY_OPERATOR);
            if (operator != 0) {
                cellTower.setOperator(operator);
            }
            position.setNetwork(new Network(cellTower));
        }
    }

    private int readExtByte(ByteBuf buf, int codec, int... codecs) {
        boolean ext = false;
        for (int c : codecs) {
            if (codec == c) {
                ext = true;
                break;
            }
        }
        if (ext) {
            return buf.readUnsignedShort();
        } else {
            return buf.readUnsignedByte();
        }
    }

    private void decodeLocation(Position position, ByteBuf buf, int codec) {

        int globalMask = 0x0f;

        if (codec == CODEC_GH3000) {

            long time = buf.readUnsignedInt() & 0x3fffffff;
            time += 1167609600; // 2007-01-01 00:00:00

            globalMask = buf.readUnsignedByte();
            if (BitUtil.check(globalMask, 0)) {

                position.setTime(new Date(time * 1000));

                int locationMask = buf.readUnsignedByte();

                if (BitUtil.check(locationMask, 0)) {
                    position.setLatitude(buf.readFloat());
                    position.setLongitude(buf.readFloat());
                }

                if (BitUtil.check(locationMask, 1)) {
                    position.setAltitude(buf.readUnsignedShort());
                }

                if (BitUtil.check(locationMask, 2)) {
                    position.setCourse(buf.readUnsignedByte() * 360.0 / 256);
                }

                if (BitUtil.check(locationMask, 3)) {
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                }

                if (BitUtil.check(locationMask, 4)) {
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                }

                if (BitUtil.check(locationMask, 5)) {
                    CellTower cellTower = CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort());

                    if (BitUtil.check(locationMask, 6)) {
                        cellTower.setSignalStrength((int) buf.readUnsignedByte());
                    }

                    if (BitUtil.check(locationMask, 7)) {
                        cellTower.setOperator(buf.readUnsignedInt());
                    }

                    position.setNetwork(new Network(cellTower));

                } else {
                    if (BitUtil.check(locationMask, 6)) {
                        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    }
                    if (BitUtil.check(locationMask, 7)) {
                        position.set(Position.KEY_OPERATOR, buf.readUnsignedInt());
                    }
                }

            } else {

                getLastLocation(position, new Date(time * 1000));

            }

        } else {

            position.setTime(new Date(buf.readLong()));

            position.set("priority", buf.readUnsignedByte());

            position.setLongitude(buf.readInt() / 10000000.0);
            position.setLatitude(buf.readInt() / 10000000.0);
            position.setAltitude(buf.readShort());
            position.setCourse(buf.readUnsignedShort());

            int satellites = buf.readUnsignedByte();
            position.set(Position.KEY_SATELLITES, satellites);

            position.setValid(satellites != 0);

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.set(Position.KEY_EVENT, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16));
            if (codec == CODEC_16) {
                buf.readUnsignedByte(); // generation type
            }

            readExtByte(buf, codec, CODEC_8_EXT); // total IO data records

        }

        // Read 1 byte data
        if (BitUtil.check(globalMask, 1)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 1, codec);
            }
        }

        // Read 2 byte data
        if (BitUtil.check(globalMask, 2)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 2, codec);
            }
        }

        // Read 4 byte data
        if (BitUtil.check(globalMask, 3)) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 4, codec);
            }
        }

        // Read 8 byte data
        if (codec == CODEC_8 || codec == CODEC_8_EXT || codec == CODEC_16) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                decodeOtherParameter(position, readExtByte(buf, codec, CODEC_8_EXT, CODEC_16), buf, 8);
            }
        }

        // Read 16 byte data
        if (extended) {
            int cnt = readExtByte(buf, codec, CODEC_8_EXT);
            for (int j = 0; j < cnt; j++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                position.set(Position.PREFIX_IO + id, ByteBufUtil.hexDump(buf.readSlice(16)));
            }
        }

        // Read X byte data
        if (codec == CODEC_8_EXT) {
            int cnt = buf.readUnsignedShort();
            for (int j = 0; j < cnt; j++) {
                int id = buf.readUnsignedShort();
                int length = buf.readUnsignedShort();
                switch (id) {
                    case 30:
                        position.set("Number of DTC (OBD)", buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                        break;
                    case 256:
                        position.set(Position.KEY_VIN, buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                        break;
                    case 264:
                        position.set("Barcode ID", buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                        break;
                    case 281:
                        position.set("Fault Codes (OBD)", buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                        break;
                    case 385:
                        ByteBuf data = buf.readSlice(length);
                        data.readUnsignedByte(); // data part
                        int index = 1;
                        while (data.isReadable()) {
                            int flags = data.readUnsignedByte();
                            if (BitUtil.from(flags, 4) > 0) {
                                position.set("beacon" + index + "Uuid", ByteBufUtil.hexDump(data.readSlice(16)));
                                position.set("beacon" + index + "Major", data.readUnsignedShort());
                                position.set("beacon" + index + "Minor", data.readUnsignedShort());
                            } else {
                                position.set("beacon" + index + "Namespace", ByteBufUtil.hexDump(data.readSlice(10)));
                                position.set("beacon" + index + "Instance", ByteBufUtil.hexDump(data.readSlice(6)));
                            }
                            position.set("beacon" + index + "Rssi", (int) data.readByte());
                            if (BitUtil.check(flags, 1)) {
                                position.set("beacon" + index + "Battery", data.readUnsignedShort() * 0.01);
                            }
                            if (BitUtil.check(flags, 2)) {
                                position.set("beacon" + index + "Temp", data.readUnsignedShort());
                            }
                            index += 1;
                        }
                        break;
                    case 387:
                        position.set("ISO6709 Coordinates", buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                        break;
                    default:
                        position.set(Position.PREFIX_IO + id, ByteBufUtil.hexDump(buf.readSlice(length)));
                        break;
                }
            }
        }

        decodeNetwork(position);

    }

    private List<Position> parseData(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf, int locationPacketId, String... imei) {
        List<Position> positions = new LinkedList<>();

        if (!connectionless) {
            buf.readUnsignedInt(); // data length
        }

        int codec = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (deviceSession == null) {
            return null;
        }

        for (int i = 0; i < count; i++) {
            Position position = new Position(getProtocolName());

            position.setDeviceId(deviceSession.getDeviceId());
            position.setValid(true);

            if (codec == CODEC_13) {
                buf.readUnsignedByte(); // type
                int length = buf.readInt() - 4;
                getLastLocation(position, new Date(buf.readUnsignedInt() * 1000));
                if (isPrintable(buf, length)) {
                    position.set(Position.KEY_RESULT,
                            buf.readCharSequence(length, StandardCharsets.US_ASCII).toString().trim());
                } else {
                    position.set(Position.KEY_RESULT,
                            ByteBufUtil.hexDump(buf.readSlice(length)));
                }
            } else if (codec == CODEC_12) {
                decodeSerial(channel, remoteAddress, position, buf);
            } else {
                decodeLocation(position, buf, codec);
            }

            if (!position.getOutdated() || !position.getAttributes().isEmpty()) {
                positions.add(position);
            }
        }

        if (channel != null && codec != CODEC_12 && codec != CODEC_13) {
            if (connectionless) {
                ByteBuf response = Unpooled.buffer();
                response.writeShort(5);
                response.writeShort(0);
                response.writeByte(0x01);
                response.writeByte(locationPacketId);
                response.writeByte(count);
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            } else {
                ByteBuf response = Unpooled.buffer();
                response.writeInt(count);
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }
        }

        return positions.isEmpty() ? null : positions;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (connectionless) {
            return decodeUdp(channel, remoteAddress, buf);
        } else {
            return decodeTcp(channel, remoteAddress, buf);
        }
    }

    private Object decodeTcp(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        if (buf.getUnsignedShort(0) > 0) {
            parseIdentification(channel, remoteAddress, buf);
        } else {
            buf.skipBytes(4);
            return parseData(channel, remoteAddress, buf, 0);
        }

        return null;
    }

    private Object decodeUdp(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // packet id
        buf.readUnsignedByte(); // packet type
        int locationPacketId = buf.readUnsignedByte();
        String imei = buf.readSlice(buf.readUnsignedShort()).toString(StandardCharsets.US_ASCII);

        return parseData(channel, remoteAddress, buf, locationPacketId, imei);

    }

}
