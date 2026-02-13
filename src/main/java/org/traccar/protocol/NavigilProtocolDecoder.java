/*
 * Copyright 2013 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class NavigilProtocolDecoder extends BaseProtocolDecoder {

    public NavigilProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final int LEAP_SECONDS_DELTA = 25;

    public static final int MSG_ERROR = 2;
    public static final int MSG_INDICATION = 4;
    public static final int MSG_CONN_OPEN = 5;
    public static final int MSG_CONN_CLOSE = 6;
    public static final int MSG_SYSTEM_REPORT = 7;
    public static final int MSG_UNIT_REPORT = 8;
    public static final int MSG_GEOFENCE_ALARM = 10;
    public static final int MSG_INPUT_ALARM = 11;
    public static final int MSG_TG2_REPORT = 12;
    public static final int MSG_POSITION_REPORT = 13;
    public static final int MSG_POSITION_REPORT_2 = 15;
    public static final int MSG_SNAPSHOT4 = 17;
    public static final int MSG_TRACKING_DATA = 18;
    public static final int MSG_MOTION_ALARM = 19;
    public static final int MSG_ACKNOWLEDGEMENT = 255;

    private static Date convertTimestamp(long timestamp) {
        return new Date((timestamp - LEAP_SECONDS_DELTA) * 1000);
    }

    private int senderSequenceNumber = 1;

    private void sendAcknowledgment(Channel channel, int sequenceNumber) {
        ByteBuf data = Unpooled.buffer(4);
        data.writeShortLE(sequenceNumber);
        data.writeShortLE(0); // OK

        ByteBuf header = Unpooled.buffer(20);
        header.writeByte(1); header.writeByte(0);
        header.writeShortLE(senderSequenceNumber++);
        header.writeShortLE(MSG_ACKNOWLEDGEMENT);
        header.writeShortLE(header.capacity() + data.capacity());
        header.writeShortLE(0);
        header.writeShortLE(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, data.nioBuffer()));
        header.writeIntLE(0);
        header.writeIntLE((int) (System.currentTimeMillis() / 1000) + LEAP_SECONDS_DELTA);

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(Unpooled.wrappedBuffer(header, data), channel.remoteAddress()));
        }
    }

    private Position parseUnitReport(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber) {
        Position position = new Position(getProtocolName());

        position.setValid(true);
        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedShortLE(); // report trigger
        position.set(Position.KEY_FLAGS, buf.readUnsignedShortLE());

        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);
        position.setAltitude(buf.readUnsignedShortLE());

        position.set(Position.KEY_SATELLITES, buf.readUnsignedShortLE());
        position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedShortLE());
        position.set("gpsAntennaState", buf.readUnsignedShortLE());

        position.setSpeed(buf.readUnsignedShortLE() * 0.194384);
        position.setCourse(buf.readUnsignedShortLE());

        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
        position.set(Position.KEY_DISTANCE, buf.readUnsignedIntLE());

        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);

        position.set(Position.KEY_CHARGE, buf.readUnsignedShortLE());

        position.setTime(convertTimestamp(buf.readUnsignedIntLE()));

        return position;
    }

    private Position parseTg2Report(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber) {
        Position position = new Position(getProtocolName());

        position.setValid(true);
        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedShortLE(); // report trigger
        buf.readUnsignedByte(); // reserved
        buf.readUnsignedByte(); // assisted GPS age

        position.setTime(convertTimestamp(buf.readUnsignedIntLE()));

        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);
        position.setAltitude(buf.readUnsignedShortLE());

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedByte());

        position.setSpeed(buf.readUnsignedShortLE() * 0.194384);
        position.setCourse(buf.readUnsignedShortLE());

        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
        position.set("maximumSpeed", buf.readUnsignedShortLE());
        position.set("minimumSpeed", buf.readUnsignedShortLE());

        position.set(Position.PREFIX_IO + 1, buf.readUnsignedShortLE()); // VSAUT1 voltage
        position.set(Position.PREFIX_IO + 2, buf.readUnsignedShortLE()); // VSAUT2 voltage
        position.set(Position.PREFIX_IO + 3, buf.readUnsignedShortLE()); // solar voltage

        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);

        return position;
    }

    private Position parsePositionReport(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber, long timestamp) {
        Position position = new Position(getProtocolName());

        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(convertTimestamp(timestamp));

        position.setLatitude(buf.readMediumLE() * 0.00002);
        position.setLongitude(buf.readMediumLE() * 0.00002);

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedByte() * 2);

        short flags = buf.readUnsignedByte();
        position.setValid((flags & 0x80) == 0x80 && (flags & 0x40) == 0x40);

        buf.readUnsignedByte(); // reserved

        return position;
    }

    private Position parsePositionReport2(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber, long timestamp) {
        Position position = new Position(getProtocolName());

        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(convertTimestamp(timestamp));

        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);

        buf.readUnsignedByte(); // report trigger

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

        short flags = buf.readUnsignedByte();
        position.setValid((flags & 0x80) == 0x80 && (flags & 0x40) == 0x40);

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

        return position;
    }

    private Position parseSnapshot4(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber) {
        Position position = new Position(getProtocolName());

        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedByte(); // report trigger
        buf.readUnsignedByte(); // position fix source
        buf.readUnsignedByte(); // GNSS fix quality
        buf.readUnsignedByte(); // GNSS assistance age

        long flags = buf.readUnsignedIntLE();
        position.setValid((flags & 0x0400) == 0x0400);

        position.setTime(convertTimestamp(buf.readUnsignedIntLE()));

        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);
        position.setAltitude(buf.readUnsignedShortLE());

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedByte());

        position.setSpeed(buf.readUnsignedShortLE() * 0.194384);
        position.setCourse(buf.readUnsignedShortLE() * 0.1);

        position.set("maximumSpeed", buf.readUnsignedByte());
        position.set("minimumSpeed", buf.readUnsignedByte());
        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

        position.set(Position.PREFIX_IO + 1, buf.readUnsignedByte()); // supply voltage 1
        position.set(Position.PREFIX_IO + 2, buf.readUnsignedByte()); // supply voltage 2
        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);

        return position;
    }

    private Position parseTrackingData(
            DeviceSession deviceSession, ByteBuf buf, int sequenceNumber, long timestamp) {
        Position position = new Position(getProtocolName());

        position.set(Position.KEY_INDEX, sequenceNumber);
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(convertTimestamp(timestamp));

        buf.readUnsignedByte(); // tracking mode

        short flags = buf.readUnsignedByte();
        position.setValid((flags & 0x01) == 0x01);

        buf.readUnsignedShortLE(); // duration

        position.setLatitude(buf.readIntLE() * 0.0000001);
        position.setLongitude(buf.readIntLE() * 0.0000001);

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedByte() * 2.0);

        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE() * 0.001);
        position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // protocol version
        buf.readUnsignedByte(); // version id
        int sequenceNumber = buf.readUnsignedShortLE();
        int messageId = buf.readUnsignedShortLE();
        buf.readUnsignedShortLE(); // length
        int flags = buf.readUnsignedShortLE();
        buf.readUnsignedShortLE(); // checksum

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(buf.readUnsignedIntLE()));
        if (deviceSession == null) {
            return null;
        }

        long timestamp = buf.readUnsignedIntLE();

        if ((flags & 0x1) == 0x0) {
            sendAcknowledgment(channel, sequenceNumber);
        }

        return switch (messageId) {
            case MSG_UNIT_REPORT -> parseUnitReport(deviceSession, buf, sequenceNumber);
            case MSG_TG2_REPORT -> parseTg2Report(deviceSession, buf, sequenceNumber);
            case MSG_POSITION_REPORT -> parsePositionReport(deviceSession, buf, sequenceNumber, timestamp);
            case MSG_POSITION_REPORT_2 -> parsePositionReport2(deviceSession, buf, sequenceNumber, timestamp);
            case MSG_SNAPSHOT4 -> parseSnapshot4(deviceSession, buf, sequenceNumber);
            case MSG_TRACKING_DATA -> parseTrackingData(deviceSession, buf, sequenceNumber, timestamp);
            default -> null;
        };
    }

}
