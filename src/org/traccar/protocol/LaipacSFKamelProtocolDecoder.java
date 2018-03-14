/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class LaipacSFKamelProtocolDecoder extends BaseProtocolDecoder {

    public LaipacSFKamelProtocolDecoder(LaipacSFKamelProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$AVRMC,")
            .expression("([^,]+),")             // identifier
            .number("(dd)(dd)(dd),")            // time (hhmmss)
            .expression("([AVRPavrp]),")        // validity
            .number("(dd)(dd.d+),")             // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.d+),")            // longitude
            .number("([EW]),")
            .number("(d+.d+),")                 // speed
            .number("(d+.d+),")                 // course
            .number("(dd)(dd)(dd),")            // date (ddmmyy)
            .expression("([abZXMHE86430]),")    // event code
            .number("(d+),")                    // battery voltage
            .number("(d+),")                    // current mileage
            .number("(d),")                     // GPS on/off (1 = on, 0 = off)
            .number("(d+),")                    // Analog port 1
            .number("(d+),")                    // Analog port 2
            .expression("([0-9a-fA-F]{4})")     // Cell 1 - Cell Net Code
            .expression("([0-9a-fA-F]{4}),")    // Cell 1 - Cell ID Code
            .number("(d{3})")                   // Cell 2 - Country Code
            .number("(d{3})")                   // Cell 2 - Operator Code
            .text("*")
            .number("(xx)")                     // checksum
            .compile();

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = (String) msg;

        if (sentence.startsWith("$ECHK") && channel != null) {
            channel.write(sentence + "\r\n"); // heartbeat
            return null;
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        String status = parser.next();
        position.setValid(status.toUpperCase().equals("A"));

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        String eventCode = parser.next();
        position.set(Position.KEY_EVENT, eventCode);
        position.set(Position.KEY_BATTERY, parser.nextDouble() * 0.001);
        position.set(Position.KEY_TOTAL_DISTANCE, parser.nextDouble());
        position.set(Position.KEY_GPS, parser.nextInt());
        position.set(Position.KEY_ANALOG_1, parser.nextDouble() * 0.001);
        position.set(Position.KEY_ANALOG_2, parser.nextDouble() * 0.001);
        position.set(Position.KEY_CELL_NET_CODE, parser.next());
        position.set(Position.KEY_CELL_ID_CODE, parser.next());
        position.set(Position.KEY_COUNTRY_CODE, parser.next());
        position.set(Position.KEY_OPERATOR, parser.next());
        String checksum = parser.next();

        if (channel != null) {
            if (Character.isLowerCase(status.charAt(0))) {
                String response = "$EAVACK," + eventCode + "," + checksum;
                response += Checksum.nmea(response);
                channel.write(response);
            }

            if (eventCode.equals("3")) {
                channel.write("$AVCFG,00000000,d*31");
            } else if (eventCode.equals("X") || eventCode.equals("4")) {
                channel.write("$AVCFG,00000000,x*2D");
            }
        }

        return position;
    }

}
