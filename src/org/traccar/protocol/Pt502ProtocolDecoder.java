/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2012 Luis Parada (luis.parada@gmail.com)
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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Pt502ProtocolDecoder extends BaseProtocolDecoder {

    private static final int MAX_CHUNK_SIZE = 960;

    private byte[] photo;

    public Pt502ProtocolDecoder(Pt502Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .any().text("$")
            .expression("([^,]+),")              // type
            .number("(d+),")                     // id
            .number("(dd)(dd)(dd).(ddd),")       // time (hhmmss.sss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.dddd),")            // latitude
            .expression("([NS]),")
            .number("(d+)(dd.dddd),")            // longitude
            .expression("([EW]),")
            .number("(d+.d+)?,")                 // speed
            .number("(d+.d+)?,")                 // course
            .number("(dd)(dd)(dd),,,")           // date (ddmmyy)
            .expression("./")
            .expression("([01])+,")              // input
            .expression("([01])+/")              // output
            .expression("([^/]+)?/")             // adc
            .number("(d+)")                      // odometer
            .expression("/([^/]+)?/")            // rfid
            .number("(xxx)").optional(2)         // state
            .any()
            .compile();

    private String decodeAlarm(String value) {
        switch (value) {
            case "TOW":
                return Position.ALARM_TOW;
            case "HDA":
                return Position.ALARM_ACCELERATION;
            case "HDB":
                return Position.ALARM_BREAKING;
            case "FDA":
                return Position.ALARM_FATIGUE_DRIVING;
            case "SKA":
                return Position.ALARM_VIBRATION;
            case "PMA":
                return Position.ALARM_MOVEMENT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        String type = parser.next();

        if (type.startsWith("PHO") && channel != null) {
            photo = new byte[Integer.parseInt(type.substring(3))];
            channel.write("#PHD0," + Math.min(photo.length, MAX_CHUNK_SIZE) + "\r\n");
        }

        position.set(Position.KEY_ALARM, decodeAlarm(type));

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextDouble(0));

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_INPUT, parser.next());
        position.set(Position.KEY_OUTPUT, parser.next());

        if (parser.hasNext()) {
            String[] values = parser.next().split(",");
            for (int i = 0; i < values.length; i++) {
                position.set(Position.PREFIX_ADC + (i + 1), Integer.parseInt(values[i], 16));
            }
        }

        position.set(Position.KEY_ODOMETER, parser.nextInt(0));
        position.set(Position.KEY_RFID, parser.next());

        if (parser.hasNext()) {
            int value = parser.nextHexInt(0);
            position.set(Position.KEY_BATTERY, value >> 8);
            position.set(Position.KEY_RSSI, (value >> 4) & 0xf);
            position.set(Position.KEY_SATELLITES, value & 0xf);
        }

        return position;
    }

}
