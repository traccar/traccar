/*
 * Copyright 2014 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
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

    public static final int MSG_COMPACT = 0x0100;
    public static final int MSG_FULL = 0x00FE;

    private static final String[] DIRECTIONS = new String[] {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int protocol = buf.readUnsignedByte();
        boolean legacy = protocol == 0x80;

        buf.readUnsignedByte(); // version id
        int index = legacy ? buf.readUnsignedShort() : buf.readUnsignedShortLE();
        int type = legacy ? buf.readUnsignedShort() : buf.readUnsignedShortLE();
        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // mask
        buf.readUnsignedShort(); // checksum
        long id = legacy ? buf.readUnsignedInt() : buf.readUnsignedIntLE();
        buf.readUnsignedInt(); // time

        Position position = new Position(getProtocolName());
        position.set(Position.KEY_INDEX, index);
        position.setValid(true);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(id));
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (protocol == 0x01 && (type == MSG_COMPACT || type == MSG_FULL)) {

            // need to send ack?

            buf.readUnsignedShortLE(); // report trigger
            buf.readUnsignedShortLE(); // state flag

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

        } else if (legacy) {

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

        return null;
    }

}
