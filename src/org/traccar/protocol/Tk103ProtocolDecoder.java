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
import org.traccar.Context;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Tk103ProtocolDecoder extends BaseProtocolDecoder {

    public Tk103ProtocolDecoder(Tk103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+)(,)?")                  // device id
            .expression(".{4},?")                // command
            .number("d*")                        // imei?
            .number("(dd)(dd)(dd),?")            // date (yymmdd)
            .expression("([AV]),?")              // validity
            .number("(dd)(dd.d+)")               // latitude
            .expression("([NS]),?")
            .number("(ddd)(dd.d+)")              // longitude
            .expression("([EW]),?")
            .number("(d+.d)(?:d*,)?")            // speed
            .number("(dd)(dd)(dd),?")            // time
            .number("(d+.?d{1,2}),?")            // course
            .number("(?:([01]{8})|(x{8}))?,?")   // state
            .number("(?:L(x+))?")                // odometer
            .any()
            .text(")").optional()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Find message start
        int beginIndex = sentence.indexOf('(');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        // Send response
        if (channel != null) {
            String id = sentence.substring(0, 12);
            String type = sentence.substring(12, 16);
            if (type.equals("BP00")) {
                String content = sentence.substring(sentence.length() - 3);
                channel.write("(" + id + "AP01" + content + ")");
            } else if (type.equals("BP05")) {
                channel.write("(" + id + "AP05)");
            }
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        DateBuilder dateBuilder = new DateBuilder();
        if (parser.next() == null) {
            dateBuilder.setDate(parser.nextInt(), parser.nextInt(), parser.nextInt());
        } else {
            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        }

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        if (Context.getConfig().getBoolean(getProtocolName() + ".mph")) {
            position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
        } else {
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        }

        dateBuilder.setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setCourse(parser.nextDouble());

        // Status
        String status = parser.next();
        if (status != null) {
            position.set(Event.KEY_STATUS, status); // binary status

            int value = Integer.parseInt(new StringBuilder(status).reverse().toString(), 2);
            position.set(Event.KEY_CHARGE, !BitUtil.check(value, 0));
            position.set(Event.KEY_IGNITION, BitUtil.check(value, 1));
        }
        position.set(Event.KEY_STATUS, parser.next()); // hex status

        if (parser.hasNext()) {
            position.set(Event.KEY_ODOMETER, parser.nextLong(16));
        }

        return position;
    }

}
