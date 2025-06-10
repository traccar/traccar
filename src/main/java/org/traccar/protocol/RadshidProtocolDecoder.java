/*
 * Copyright 2020 - 2021 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.helper.Checksum;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class RadshidProtocolDecoder extends BaseProtocolDecoder {

    private static final Checksum.Algorithm CRC16_CCITT_A001 = new Checksum.Algorithm(
        16,
        0xA001,
        0x0000,
        false,
        false,
        0x0000
        );

    public RadshidProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static void sendResponse(
            Channel channel, SocketAddress remoteAddress, int length, Boolean crcStatus) {

        if (channel != null) {
            ByteBuf buf = Unpooled.buffer();

            buf.writeIntLE(0x07);
            buf.writeByte(0x82);
            buf.writeByte(0x01);
            buf.writeInt(length);
            buf.writeByte(crcStatus ? 0x01 : 0x00);

            channel.writeAndFlush(new NetworkMessage(buf, remoteAddress));
        }
    }

    private String decodeAlarm(int event) {
        switch (event) {
            case 0xD6: // GSM_Jamming_Event
            case 0xD7: // GPS_Jamming_Event
                return Position.ALARM_JAMMING;
            case 0xEB: // SOS_PressEvent
            case 0xEC: // SOS_ReleaseEvent
                return Position.ALARM_SOS;
            case 0xF0: // Battry_Level_Event
                return Position.ALARM_LOW_BATTERY;
            case 0xDC: // GeofenceEnable_Event
                return Position.ALARM_GEOFENCE_ENTER;
            case 0xDD: // GeofenceDisable_Event
                return Position.ALARM_GEOFENCE_EXIT;
            case 0xEE: // OverSpeedEvent
                return Position.ALARM_OVERSPEED;
            case 0xE2: // DisconnetPowerLineEvent
                return Position.ALARM_POWER_CUT;
            case 0xE3: // ConnectPowerLineEvent
                return Position.ALARM_POWER_RESTORED;
            case 0xE5: // AccelerationEvent
                return Position.ALARM_ACCELERATION;

            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        int length = buf.readableBytes();

        /* read crc from end of packet */
        buf.readerIndex(length - 2);
        int packetChecksum = buf.readUnsignedShortLE();
        buf.readerIndex(0);

        /* copy data part of packet */
        ByteBuf packetData = buf.slice(0, buf.readableBytes() - 2);

        /* calc data Checksum*/
        int dataChecksum = Checksum.crc16(CRC16_CCITT_A001, packetData.nioBuffer());

        /* check data crc and packet crc */
        if (dataChecksum != packetChecksum) {
            /* send response to device */
            sendResponse(channel, remoteAddress, length, false);
            return null;
        }

        byte packetTag = packetData.readByte(); // Packet Tag
        long deviceSerial = (long) packetData.readUnsignedInt();
        byte dataElement = packetData.readByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(deviceSerial));
        if (deviceSession == null || packetTag != 0x01) {
            return null;
        }

        /* Send response to device */
        sendResponse(channel, remoteAddress, length, true);

        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < dataElement; i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            packetData.readByte(); // Event Version
            byte event = packetData.readByte(); // Event Code
            packetData.readBytes(5); // Driver Id
            packetData.readUnsignedShort(); // Total Driving Tim
            long timeStamp = packetData.readUnsignedInt();  // Timestamp
            byte gpsSpeed = packetData.readByte();  // GPS Speed
            packetData.readByte();  // Vehicle Sensor Speed
            packetData.readByte();  // Max Speed (GPS)
            packetData.readByte();  // Max Speed (Vehicle Sensor)
            packetData.readUnsignedShort(); // Engine RPM
            packetData.readUnsignedInt();   // Total Traveled Distance (GPS)
            packetData.readUnsignedInt();   // otal Traveled Distance (Vehicle Sensor)
            byte ioStatus = packetData.readByte();  // IO Status
            byte gpsStatus = packetData.readByte(); // GPS Status
            long latitude = packetData.readInt();   // Latitude
            long longitude = packetData.readInt();   // Longitude
            int altitude = packetData.readShort();  //Altitude
            int bearing = packetData.readShort();   //Bearing
            byte numberOfSatellites = packetData.readByte(); // Number Of Satellites
            byte pdop = packetData.readByte(); // PDOP
            byte xLen = packetData.readByte(); // Extra Data Length
            packetData.readBytes(xLen); // scape extra data

            position.setValid((gpsStatus == 0x00));
            position.setTime(new Date(timeStamp * 1000L));
            position.setLatitude(latitude * 0.0000001);
            position.setLongitude(longitude * 0.0000001);
            position.setAltitude(altitude);
            position.setSpeed(UnitsConverter.knotsFromKph(gpsSpeed));
            position.setCourse(bearing);
            position.set(Position.KEY_SATELLITES, numberOfSatellites);
            position.set(Position.KEY_PDOP, pdop * 0.1);
            position.set(Position.KEY_IGNITION, (ioStatus & 0x40) != 0);
            position.set(Position.KEY_ALARM, decodeAlarm((int) event));

            positions.add(position);
        }
        return positions;
    }

}
