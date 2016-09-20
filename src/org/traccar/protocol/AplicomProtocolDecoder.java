/*
 * Copyright 2013 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

public class AplicomProtocolDecoder extends BaseProtocolDecoder {

    public AplicomProtocolDecoder(AplicomProtocol protocol) {
        super(protocol);
    }

    private static final long IMEI_BASE_TC65_V20 = 0x1437207000000L;
    private static final long IMEI_BASE_TC65_V28 = 358244010000000L;
    private static final long IMEI_BASE_TC65I_V11 = 0x14143B4000000L;

    private static boolean validateImei(long imei) {
        return Checksum.luhn(imei / 10) == imei % 10;
    }

    private static long imeiFromUnitId(long unitId) {

        if (unitId == 0) {

            return 0;

        } else {

            // Try TC65i
            long imei = IMEI_BASE_TC65I_V11 + unitId;
            if (validateImei(imei)) {
                return imei;
            }

            // Try TC65 v2.8
            imei = IMEI_BASE_TC65_V28 + ((unitId + 0xA8180) & 0xFFFFFF);
            if (validateImei(imei)) {
                return imei;
            }

            // Try TC65 v2.0
            imei = IMEI_BASE_TC65_V20 + unitId;
            if (validateImei(imei)) {
                return imei;
            }

        }

        return unitId;
    }

    private static final int DEFAULT_SELECTOR_D = 0x0002FC;
    private static final int DEFAULT_SELECTOR_E = 0x007ffc;

    private static final int EVENT_DATA = 119;

    private void decodeEventData(int event, ChannelBuffer buf) {
        switch (event) {
            case 2:
            case 40:
                buf.readUnsignedByte();
                break;
            case 9:
                buf.readUnsignedMedium();
                break;
            case 31:
            case 32:
                buf.readUnsignedShort();
                break;
            case 38:
                buf.skipBytes(4 * 9);
                break;
            case 113:
                buf.readUnsignedInt();
                buf.readUnsignedByte();
                break;
            case 121:
            case 142:
                buf.readLong();
                break;
            case 130:
                buf.readUnsignedInt(); // incorrect
                break;
            default:
                break;
        }
    }

    private void decodeCanData(ChannelBuffer buf, Position position) {

        buf.readUnsignedMedium(); // packet identifier
        buf.readUnsignedByte(); // version
        int count = buf.readUnsignedByte();
        buf.readUnsignedByte(); // batch count
        buf.readUnsignedShort(); // selector bit
        buf.readUnsignedInt(); // timestamp

        buf.skipBytes(8);

        ArrayList<ChannelBuffer> values = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            values.add(buf.readBytes(8));
        }

        for (int i = 0; i < count; i++) {
            ChannelBuffer value = values.get(i);
            switch (buf.readInt()) {
                case 0x20D:
                    position.set(Position.KEY_RPM, ChannelBuffers.swapShort(value.readShort()));
                    position.set("dieselTemperature", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    position.set("batteryVoltage", ChannelBuffers.swapShort(value.readShort()) * 0.01);
                    position.set("supplyAirTempDep1", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    break;
                case 0x30D:
                    position.set("activeAlarm", ChannelBuffers.hexDump(value));
                    break;
                case 0x40C:
                    position.set("airTempDep1", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    position.set("airTempDep2", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    break;
                case 0x40D:
                    position.set("coldUnitState", ChannelBuffers.hexDump(value));
                    break;
                case 0x50C:
                    position.set("defrostTempDep1", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    position.set("defrostTempDep2", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    break;
                case 0x50D:
                    position.set("condenserPressure", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    position.set("suctionPressure", ChannelBuffers.swapShort(value.readShort()) * 0.1);
                    break;
                case 0x58C:
                    value.readByte();
                    value.readShort(); // index
                    switch (value.readByte()) {
                        case 0x01:
                            position.set("setpointZone1", ChannelBuffers.swapInt(value.readInt()) * 0.1);
                            break;
                        case 0x02:
                            position.set("setpointZone2", ChannelBuffers.swapInt(value.readInt()) * 0.1);
                            break;
                        case 0x05:
                            position.set("unitType", ChannelBuffers.swapInt(value.readInt()));
                            break;
                        case 0x13:
                            position.set("dieselHours", ChannelBuffers.swapInt(value.readInt()) / 60 / 60);
                            break;
                        case 0x14:
                            position.set("electricHours", ChannelBuffers.swapInt(value.readInt()) / 60 / 60);
                            break;
                        case 0x17:
                            position.set("serviceIndicator", ChannelBuffers.swapInt(value.readInt()));
                            break;
                        case 0x18:
                            position.set("softwareVersion", ChannelBuffers.swapInt(value.readInt()) * 0.01);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    Log.warning(new UnsupportedOperationException());
                    break;
            }
        }
    }

    private void decodeD(Position position, ChannelBuffer buf, int selector, int event) {

        if ((selector & 0x0008) != 0) {
            position.setValid((buf.readUnsignedByte() & 0x40) != 0);
        } else {
            getLastLocation(position, null);
        }

        if ((selector & 0x0004) != 0) {
            buf.skipBytes(4); // snapshot time
        }

        if ((selector & 0x0008) != 0) {
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            position.setLatitude(buf.readInt() / 1000000.0);
            position.setLongitude(buf.readInt() / 1000000.0);
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if ((selector & 0x0010) != 0) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            buf.readUnsignedByte(); // maximum speed
            position.setCourse(buf.readUnsignedByte() * 2.0);
        }

        if ((selector & 0x0040) != 0) {
            position.set(Position.KEY_INPUT, buf.readUnsignedByte());
        }

        if ((selector & 0x0020) != 0) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 3, buf.readUnsignedShort());
            position.set(Position.PREFIX_ADC + 4, buf.readUnsignedShort());
        }

        if ((selector & 0x8000) != 0) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() / 1000.0);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort());
        }

        // Pulse rate 1
        if ((selector & 0x10000) != 0) {
            buf.readUnsignedShort();
            buf.readUnsignedInt();
        }

        // Pulse rate 2
        if ((selector & 0x20000) != 0) {
            buf.readUnsignedShort();
            buf.readUnsignedInt();
        }

        if ((selector & 0x0080) != 0) {
            position.set("trip1", buf.readUnsignedInt());
        }

        if ((selector & 0x0100) != 0) {
            position.set("trip2", buf.readUnsignedInt());
        }

        if ((selector & 0x0040) != 0) {
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());
        }

        if ((selector & 0x0200) != 0) {
            position.set(Position.KEY_RFID, (((long) buf.readUnsignedShort()) << 32) + buf.readUnsignedInt());
        }

        if ((selector & 0x0400) != 0) {
            buf.readUnsignedByte(); // Keypad
        }

        if ((selector & 0x0800) != 0) {
            position.setAltitude(buf.readShort());
        }

        if ((selector & 0x2000) != 0) {
            buf.readUnsignedShort(); // snapshot counter
        }

        if ((selector & 0x4000) != 0) {
            buf.skipBytes(8); // state flags
        }

        if ((selector & 0x80000) != 0) {
            buf.skipBytes(11); // cell info
        }

        if ((selector & 0x1000) != 0) {
            decodeEventData(event, buf);
        }

        if (Context.getConfig().getBoolean(getProtocolName() + ".can")
                && buf.readable() && (selector & 0x1000) != 0 && event == EVENT_DATA) {
            decodeCanData(buf, position);
        }
    }

    private void decodeE(Position position, ChannelBuffer buf, int selector) {

        if ((selector & 0x0008) != 0) {
            position.set("tachographEvent", buf.readUnsignedShort());
        }

        if ((selector & 0x0004) != 0) {
            getLastLocation(position, new Date(buf.readUnsignedInt() * 1000));
        } else {
            getLastLocation(position, null);
        }

        if ((selector & 0x0010) != 0) {
            String time =
                    buf.readUnsignedByte() + "s " + buf.readUnsignedByte() + "m " + buf.readUnsignedByte() + "h " +
                    buf.readUnsignedByte() + "M " + buf.readUnsignedByte() + "D " + buf.readUnsignedByte() + "Y " +
                    buf.readByte() + "m " + buf.readByte() + "h";
            position.set("tachographTime", time);
        }

        position.set("workState", buf.readUnsignedByte());
        position.set("driver1State", buf.readUnsignedByte());
        position.set("driver2State", buf.readUnsignedByte());

        if ((selector & 0x0020) != 0) {
            position.set("tachographStatus", buf.readUnsignedByte());
        }

        if ((selector & 0x0040) != 0) {
            position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShort() / 256.0);
        }

        if ((selector & 0x0080) != 0) {
            position.set(Position.KEY_OBD_ODOMETER, buf.readUnsignedInt() * 5);
        }

        if ((selector & 0x0100) != 0) {
            position.set(Position.KEY_TRIP_ODOMETER, buf.readUnsignedInt() * 5);
        }

        if ((selector & 0x8000) != 0) {
            position.set("kFactor", buf.readUnsignedShort() * 0.001 + " pulses/m");
        }

        if ((selector & 0x0200) != 0) {
            position.set(Position.KEY_RPM, buf.readUnsignedShort() * 0.125);
        }

        if ((selector & 0x0400) != 0) {
            position.set("extraInfo", buf.readUnsignedShort());
        }

        if ((selector & 0x0800) != 0) {
            position.set(Position.KEY_VIN, buf.readBytes(18).toString(StandardCharsets.US_ASCII).trim());
        }
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        char protocol = (char) buf.readByte();
        int version = buf.readUnsignedByte();

        String imei;
        if ((version & 0x80) != 0) {
            imei = String.valueOf((buf.readUnsignedInt() << (3 * 8)) | buf.readUnsignedMedium());
        } else {
            imei = String.valueOf(imeiFromUnitId(buf.readUnsignedMedium()));
        }

        buf.readUnsignedShort(); // length

        int selector = DEFAULT_SELECTOR_D;
        if (protocol == 'E') {
            selector = DEFAULT_SELECTOR_E;
        }
        if ((version & 0x40) != 0) {
            selector = buf.readUnsignedMedium();
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int event = buf.readUnsignedByte();
        position.set(Position.KEY_EVENT, event);
        position.set("eventInfo", buf.readUnsignedByte());

        if (protocol == 'D') {
            decodeD(position, buf, selector, event);
        } else if (protocol == 'E') {
            decodeE(position, buf, selector);
        } else {
            return null;
        }

        return position;
    }

}
