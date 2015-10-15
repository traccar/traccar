/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class AquilaProtocolDecoder extends BaseProtocolDecoder {

    public AquilaProtocolDecoder(AquilaProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .txt("$$")
            .nxt(",")                            // client
            .num("(d+),")                        // device serial number
            .num("(d+),")                        // event
            .num("(-?d+.d+),")                   // latitude
            .num("(-?d+.d+),")                   // longitude
            .num("(dd)(dd)(dd)")                 // date (yymmdd)
            .num("(dd)(dd)(dd),")                // time (hhmmss)
            .xpr("([AV]),")                      // validity
            .num("(d+),")                        // gsm
            .num("(d+),")                        // speed
            .num("(d+),")                        // distance
            .num("d+,")                          // driver code
            .num("(d+),")                        // fuel
            .num("([01]),")                      // io 1
            .num("[01],")                        // case open switch
            .num("[01],")                        // over speed start
            .num("[01],")                        // over speed end
            .num("(?:d+,){3}")                   // reserved
            .num("([01]),")                      // power status
            .num("([01]),")                      // io 2
            .num("d+,")                          // reserved
            .num("([01]),")                      // ignition
            .num("[01],")                        // ignition off event
            .num("(?:d+,){7}")                   // reserved
            .num("[01],")                        // corner packet
            .num("(?:d+,){8}")                   // reserved
            .num("([01]),")                      // course bit 0
            .num("([01]),")                      // course bit 1
            .num("([01]),")                      // course bit 2
            .num("([01]),")                      // course bit 3
            .txt("*")
            .num("(xx)")                         // checksum
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set(Event.KEY_EVENT, parser.nextInt());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setValid(parser.next().equals("A"));

        position.set(Event.KEY_GSM, parser.nextInt());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Event.KEY_ODOMETER, parser.next());
        position.set(Event.KEY_FUEL, parser.next());
        position.set(Event.PREFIX_IO + 1, parser.next());
        position.set(Event.KEY_CHARGE, parser.next());
        position.set(Event.PREFIX_IO + 2, parser.next());

        position.set(Event.KEY_IGNITION, parser.nextInt() == 1);

        int course = (parser.nextInt() << 3) + (parser.nextInt() << 2) + (parser.nextInt() << 1) + parser.nextInt();
        if (course > 0 && course <= 8) {
            position.setCourse((course - 1) * 45);
        }

        return position;
    }

}
