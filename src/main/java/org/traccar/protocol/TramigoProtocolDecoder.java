/*
 * Copyright 2014 - 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TramigoProtocolDecoder extends BaseProtocolDecoder {

    public TramigoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final String[] DIRECTIONS = new String[] {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int protocol = buf.readUnsignedByte();

        if (protocol == 0x01) {
            return decode01(channel, remoteAddress, buf);
        } else if (protocol == 0x04) {
            return decode04(channel, remoteAddress, buf);
        } else if (protocol == 0x80) {
            return decode80(channel, remoteAddress, buf);
        }

        return null;
    }

    private Position decode01(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedByte(); // version id
        int index = buf.readUnsignedShortLE();
        int type = buf.readUnsignedShortLE();

        if (type == 0x0100 || type == 0x00FE) {

            buf.readUnsignedShort(); // length
            buf.readUnsignedShort(); // mask
            buf.readUnsignedShort(); // checksum
            long id = buf.readUnsignedIntLE();
            buf.readUnsignedInt(); // time

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.set(Position.KEY_INDEX, index);

            // need to send ack?

            buf.readUnsignedShortLE(); // report trigger
            buf.readUnsignedShortLE(); // state flag

            position.setValid(true);
            position.setLatitude(buf.readUnsignedIntLE() * 0.0000001);
            position.setLongitude(buf.readUnsignedIntLE() * 0.0000001);

            position.set(Position.KEY_RSSI, buf.readUnsignedShortLE());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedShortLE());
            position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedShortLE());
            position.set("gpsAntennaStatus", buf.readUnsignedShortLE());

            position.setSpeed(buf.readUnsignedShortLE() * 0.194384);
            position.setCourse(buf.readUnsignedShortLE());

            position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());

            position.set(Position.KEY_BATTERY, buf.readUnsignedShortLE());

            position.set(Position.KEY_CHARGE, buf.readUnsignedShortLE());

            position.setTime(new Date(buf.readUnsignedIntLE() * 1000));

            // parse other data

            return position;

        }

        return null;

    }

    private Position decode04(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedShortLE(); // length
        buf.readUnsignedShortLE(); // checksum
        int index = buf.readUnsignedShortLE();
        long id1 = buf.readUnsignedIntLE();
        long id2 = buf.readUnsignedIntLE();
        long time = buf.readUnsignedIntLE();

        String id = String.format("%08d%07d", id1, id2);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x04); // protocol
            response.writeShortLE(24); // length
            response.writeShortLE(0); // checksum
            response.writeShortLE(index);
            response.writeIntLE((int) id1);
            response.writeIntLE((int) id2);
            response.writeIntLE((int) time);

            response.writeByte(0xff); // acknowledgement
            response.writeShortLE(index);
            response.writeShortLE(0); // success

            response.setShortLE(3, Checksum.crc16(Checksum.CRC16_CCITT_FALSE, response.nioBuffer()));

            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_INDEX, index);

        position.setDeviceTime(new Date(time * 1000));

        while (buf.isReadable()) {
            int type = buf.readUnsignedByte();
            switch (type) {
                case 0 -> {
                    position.set(Position.KEY_EVENT, buf.readUnsignedShortLE());
                    buf.readUnsignedIntLE(); // event data

                    int status = buf.readUnsignedShortLE();
                    position.set(Position.KEY_IGNITION, BitUtil.check(status, 5));
                    position.set(Position.KEY_STATUS, status);

                    position.setValid(true);
                    position.setLatitude(buf.readIntLE() * 0.00001);
                    position.setLongitude(buf.readIntLE() * 0.00001);
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShortLE()));
                    position.setCourse(buf.readUnsignedShortLE());

                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    position.set(Position.KEY_GPS, buf.readUnsignedByte());
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    position.set(Position.KEY_ODOMETER_TRIP, buf.readUnsignedShortLE());
                    position.set("maxAcceleration", buf.readUnsignedShortLE() * 0.001);
                    position.set("maxDeceleration", buf.readUnsignedShortLE() * 0.001);
                    buf.readUnsignedShortLE(); // bearing to landmark
                    buf.readUnsignedIntLE(); // distance to landmark

                    position.setFixTime(new Date(buf.readUnsignedIntLE() * 1000));

                    buf.readUnsignedByte(); // reserved
                }
                case 1 -> buf.skipBytes(buf.readUnsignedShortLE() - 3); // landmark
                case 4 -> buf.skipBytes(53); // trip
                case 20 -> buf.skipBytes(32); // extended
                case 22 -> {
                    buf.readUnsignedByte(); // zone flag
                    buf.skipBytes(buf.readUnsignedShortLE()); // zone name
                }
                case 30 -> buf.skipBytes(79); // system status
                case 40 -> buf.skipBytes(40); // analog
                case 50 -> buf.skipBytes(buf.readUnsignedShortLE() - 3); // console
                case 255 -> buf.skipBytes(4); // acknowledgement
                default -> throw new IllegalArgumentException(String.format("Unknown type %d", type));
            }
        }

        return position.getValid() ? position : null;

    }

    private Position decode80(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws ParseException {

        buf.readUnsignedByte(); // version id
        int index = buf.readUnsignedShort();
        buf.readUnsignedShort(); // type

        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // mask
        buf.readUnsignedShort(); // checksum
        long id = buf.readUnsignedInt();
        buf.readUnsignedInt(); // time

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_INDEX, index);

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer("gprs,ack," + index, StandardCharsets.US_ASCII), remoteAddress));
        }

        String sentence = buf.toString(StandardCharsets.US_ASCII);

        Pattern pattern = Pattern.compile("(-?\\d+\\.\\d+), (-?\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(sentence);
        if (!matcher.find()) {
            return null;
        }
        position.setLatitude(Double.parseDouble(matcher.group(1)));
        position.setLongitude(Double.parseDouble(matcher.group(2)));
        position.setValid(true);

        pattern = Pattern.compile("([NSWE]{1,2}) with speed (\\d+) km/h");
        matcher = pattern.matcher(sentence);
        if (matcher.find()) {
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if (matcher.group(1).equals(DIRECTIONS[i])) {
                    position.setCourse(i * 45.0);
                    break;
                }
            }
            position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(matcher.group(2))));
        }

        pattern = Pattern.compile("(\\d{1,2}:\\d{2}(:\\d{2})? \\w{3} \\d{1,2})");
        matcher = pattern.matcher(sentence);
        if (!matcher.find()) {
            return null;
        }
        DateFormat dateFormat = new SimpleDateFormat(
                matcher.group(2) != null ? "HH:mm:ss MMM d yyyy" : "HH:mm MMM d yyyy", Locale.ENGLISH);
        position.setTime(DateUtil.correctYear(
                dateFormat.parse(matcher.group(1) + " " + Calendar.getInstance().get(Calendar.YEAR))));

        if (sentence.contains("Ignition on detected")) {
            position.set(Position.KEY_IGNITION, true);
        } else if (sentence.contains("Ignition off detected")) {
            position.set(Position.KEY_IGNITION, false);
        }

        return position;

    }

}
