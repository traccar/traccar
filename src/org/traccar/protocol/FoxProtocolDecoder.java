/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FoxProtocolDecoder extends BaseProtocolDecoder {

    public FoxProtocolDecoder(FoxProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                     // status id
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // date
            .number("(dd)(dd)(dd),")             // time
            .number("(dd)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")             // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .expression("[^,]*,")                // cell info
            .number("([01]+) ")                  // input
            .number("(d+) ")                     // power
            .number("(d+) ")                     // temperature
            .number("(d+) ")                     // rpm
            .number("(d+) ")                     // fuel
            .number("(d+) ")                     // adc 1
            .number("(d+) ")                     // adc 2
            .number("([01]+) ")                  // output
            .number("(d+),")                     // odometer
            .expression("(.+)")                  // status info
            .compile();

    private String getAttribute(String xml, String key) {
        int start = xml.indexOf(key + "=\"");
        if (start != -1) {
            start += key.length() + 2;
            int end = xml.indexOf("\"", start);
            if (end != -1) {
                return xml.substring(start, end);
            }
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String xml = (String) msg;
        String id = getAttribute(xml, "id");
        String data = getAttribute(xml, "data");

        if (id != null && data != null) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
            if (deviceSession == null) {
                return null;
            }

            Parser parser = new Parser(PATTERN, data);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_STATUS, parser.nextInt());

            position.setValid(parser.next().equals("A"));

            DateBuilder dateBuilder = new DateBuilder()
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());

            position.set(Position.KEY_INPUT, parser.nextInt(2));
            position.set(Position.KEY_POWER, parser.nextDouble() * 0.1);
            position.set(Position.PREFIX_TEMP + 1, parser.nextInt());
            position.set(Position.KEY_RPM, parser.nextInt());
            position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());
            position.set(Position.PREFIX_ADC + 1, parser.nextInt());
            position.set(Position.PREFIX_ADC + 2, parser.nextInt());
            position.set(Position.KEY_OUTPUT, parser.nextInt(2));
            position.set(Position.KEY_ODOMETER, parser.nextInt());

            position.set("statusData", parser.next());

            return position;

        }

        return null;
    }

}
