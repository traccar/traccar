/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class RitiProtocolDecoder extends BaseProtocolDecoder {

    public RitiProtocolDecoder(RitiProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$GPRMC,")
            .number("(dd)(dd)(dd).?d*,")         // time
            .expression("([AV]),")               // validity
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(String.valueOf(buf.readUnsignedShort()), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set("mode", buf.readUnsignedByte());
        position.set("command", buf.readUnsignedByte());
        position.set(Event.KEY_POWER, buf.readUnsignedShort());

        buf.skipBytes(5);
        buf.readUnsignedShort();
        buf.readUnsignedShort();

        position.set("distance", buf.readUnsignedInt());
        position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());

        // Parse GPRMC
        int end = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '*');
        String gprmc = buf.toString(buf.readerIndex(), end - buf.readerIndex(), Charset.defaultCharset());
        Parser parser = new Parser(PATTERN, gprmc);
        if (!parser.matches()) {
            return null;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        return position;
    }

}
