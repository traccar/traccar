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

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Gps103ProtocolDecoder extends BaseProtocolDecoder {

    public Gps103ProtocolDecoder(Gps103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("imei:")
            .number("(d+),")                     // imei
            .expression("([^,]+),")              // alarm
            .number("(dd)/?(dd)/?(dd) ?")        // local date
            .number("(dd):?(dd)(?:dd)?,")        // local time
            .expression("[^,]*,")
            .expression("[FL],")                 // full / low
            .groupBegin()
            .number("(dd)(dd)(dd).(d+)")         // time utc (hhmmss.sss)
            .or()
            .number("(?:d{1,5}.d+)?")
            .groupEnd()
            .text(",")
            .expression("([AV]),")               // validity
            .expression("([NS]),").optional()
            .number("(d+)(dd.d+),")              // latitude (ddmm.mmmm)
            .expression("([NS]),").optional()
            .expression("([EW]),").optional()
            .number("(d+)(dd.d+),")              // longitude (dddmm.mmmm)
            .expression("([EW])?,").optional()
            .number("(d+.?d*)?,?")               // speed
            .number("(d+.?d*)?,?")               // course
            .number("(d+.?d*)?,?")               // altitude
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .expression("([^,;]+)?,?")
            .any()
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .text("imei:")
            .number("(d+),")                     // imei
            .expression("[^,]+,")                // alarm
            .number("d+,,")
            .text("L,,,")
            .number("(x+),,")                    // lac
            .number("(x+),,,")                   // cid
            .any()
            .compile();

    private static final Pattern PATTERN_HANDSHAKE = new PatternBuilder()
            .number("##,imei:(d+),A")
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            if (channel != null) {
                channel.write("LOAD", remoteAddress);
                Parser handshakeParser = new Parser(PATTERN_HANDSHAKE, sentence);
                if (handshakeParser.matches()) {
                    identify(handshakeParser.next(), channel);
                }
            }
            return null;
        }

        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            if (channel != null) {
                channel.write("ON", remoteAddress);
            }
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        Parser parser = new Parser(PATTERN_NETWORK, sentence);
        if (parser.matches()) {

            if (!identify(parser.next(), channel, remoteAddress)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            getLastLocation(position, null);

            position.set(Event.KEY_LAC, parser.nextInt(16));
            position.set(Event.KEY_CID, parser.nextInt(16));

            return position;

        }

        parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        String alarm = parser.next();
        position.set(Event.KEY_ALARM, alarm);
        if (channel != null && alarm.equals("help me")) {
            channel.write("**,imei:" + imei + ",E;", remoteAddress);
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt());

        int localHours = parser.nextInt();
        int localMinutes = parser.nextInt();

        String utcHours = parser.next();
        String utcMinutes = parser.next();

        dateBuilder.setTime(localHours, localMinutes, parser.nextInt(), parser.nextInt());

        // Timezone calculation
        if (utcHours != null && utcMinutes != null) {
            int deltaMinutes = (localHours - Integer.parseInt(utcHours)) * 60;
            deltaMinutes += localMinutes - Integer.parseInt(utcMinutes);
            if (deltaMinutes <= -12 * 60) {
                deltaMinutes += 24 * 60;
            } else if (deltaMinutes > 12 * 60) {
                deltaMinutes -= 24 * 60;
            }
            dateBuilder.addMinute(-deltaMinutes);
        }
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN_HEM));
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());
        position.setAltitude(parser.nextDouble());

        for (int i = 1; i <= 5; i++) {
            position.set(Event.PREFIX_IO + i, parser.next());
        }

        return position;
    }

}
