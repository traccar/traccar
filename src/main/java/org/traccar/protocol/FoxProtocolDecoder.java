/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FoxProtocolDecoder extends BaseProtocolDecoder {

    public FoxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                     // status id
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
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

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_STATUS, parser.nextInt(0));

            position.setValid(parser.next().equals("A"));

            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.setCourse(parser.nextDouble(0));

            position.set(Position.KEY_INPUT, parser.nextBinInt(0));
            position.set(Position.KEY_POWER, parser.nextDouble(0) * 0.1);
            position.set(Position.PREFIX_TEMP + 1, parser.nextInt(0));
            position.set(Position.KEY_RPM, parser.nextInt(0));
            position.set(Position.KEY_FUEL_LEVEL, parser.nextInt(0));
            position.set(Position.PREFIX_ADC + 1, parser.nextInt(0));
            position.set(Position.PREFIX_ADC + 2, parser.nextInt(0));
            position.set(Position.KEY_OUTPUT, parser.nextBinInt(0));
            position.set(Position.KEY_ODOMETER, parser.nextInt(0));

            position.set("statusData", parser.next());

            return position;

        }

        return null;
    }

}
