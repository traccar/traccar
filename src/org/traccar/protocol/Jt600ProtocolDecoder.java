/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.BcdUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Jt600ProtocolDecoder extends BaseProtocolDecoder {

    public Jt600ProtocolDecoder(Jt600Protocol protocol) {
        super(protocol);
    }

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    private Position decodeNormalMessage(ChannelBuffer buf, Channel channel, SocketAddress remoteAddress) {

        Position position = new Position();
        position.setProtocol(getProtocolName());

        buf.readByte(); // header

        String id = String.valueOf(Long.parseLong(ChannelBuffers.hexDump(buf.readBytes(5))));
        if (!identify(id, channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        int version = BcdUtil.readInteger(buf, 1);
        buf.readUnsignedByte(); // type
        buf.readBytes(2); // length

        DateBuilder dateBuilder = new DateBuilder()
                .setDay(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setYear(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        position.setTime(dateBuilder.getDate());

        double latitude = convertCoordinate(BcdUtil.readInteger(buf, 8));
        double longitude = convertCoordinate(BcdUtil.readInteger(buf, 9));

        byte flags = buf.readByte();
        position.setValid((flags & 0x1) == 0x1);
        if ((flags & 0x2) == 0) {
            latitude = -latitude;
        }
        position.setLatitude(latitude);
        if ((flags & 0x4) == 0) {
            longitude = -longitude;
        }
        position.setLongitude(longitude);

        position.setSpeed(BcdUtil.readInteger(buf, 2));
        position.setCourse(buf.readUnsignedByte() * 2.0);

        if (version == 1) {

            position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
            position.set(Event.KEY_POWER, buf.readUnsignedByte());

            buf.readByte(); // other flags and sensors

            position.setAltitude(buf.readUnsignedShort());

            int cid = buf.readUnsignedShort();
            int lac = buf.readUnsignedShort();
            if (cid != 0 && lac != 0) {
                position.set(Event.KEY_CID, cid);
                position.set(Event.KEY_LAC, lac);
            }

            position.set(Event.KEY_GSM, buf.readUnsignedByte());

        } else if (version == 2) {

            int fuel = buf.readUnsignedByte() << 8;

            position.set(Event.KEY_STATUS, buf.readUnsignedInt());
            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());

            fuel += buf.readUnsignedByte();
            position.set(Event.KEY_FUEL, fuel);

        }

        return position;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("(")
            .number("(d+),")                     // id
            .text("W01,")                        // type
            .number("(ddd)(dd.dddd),")           // longitude
            .expression("([EW]),")
            .number("(dd)(dd.dddd),")            // latitude
            .expression("([NS]),")
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(d+),")                     // power
            .number("(d+),")                     // gps signal
            .number("(d+),")                     // gsm signal
            .number("(d+),")                     // alert type
            .any()
            .text(")")
            .compile();

    private Position decodeAlertMessage(ChannelBuffer buf, Channel channel, SocketAddress remoteAddress) {

        Parser parser = new Parser(PATTERN, buf.toString(StandardCharsets.US_ASCII));
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        position.set(Event.KEY_ALARM, true);

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.setLongitude(parser.nextCoordinate());
        position.setLatitude(parser.nextCoordinate());
        position.setValid(parser.next().equals("A"));

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Event.KEY_POWER, parser.nextDouble());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        char first = (char) buf.getByte(0);

        if (first == '$') {
            return decodeNormalMessage(buf, channel, remoteAddress);
        } else if (first == '(') {
            return decodeAlertMessage(buf, channel, remoteAddress);
        }

        return null;
    }

}
