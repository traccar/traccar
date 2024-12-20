/*
 * Copyright 2013 - 2024 Anton Tananaev (anton@traccar.org)
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

public class GotopProtocolDecoder extends BaseProtocolDecoder {

    public GotopProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+),")                     // imei
            .expression("([^,]+),")              // type
            .expression("([AV]),")               // validity
            .text("DATE:").optional()
            .number("(dd)(dd)(dd),")             // date (yyddmm)
            .text("TIME:").optional()
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .text("LAT:").optional()
            .number("(d+.d+)([NS]),")            // latitude
            .expression("LO[NT]:").optional()
            .number("(d+.d+)([EW]),")            // longitude
            .text("Speed:").number("(d+.d+),")   // speed
            .groupBegin()
            .number("(d+)-")                     // battery
            .number("(d+),")                     // rssi
            .groupBegin()
            .number("(d+.d),")                   // altitude
            .number("(d+.dd)")                   // hdop
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String type = parser.next();
        if (type.equals("CMD-KEY")) {
            position.addAlarm(Position.ALARM_SOS);
        } else if (type.startsWith("ALM-B")) {
            if (Character.getNumericValue(type.charAt(5)) % 2 > 0) {
                position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
            } else {
                position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
            }
        }

        position.setValid(parser.next().equals("A"));

        position.setTime(parser.nextDateTime());

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.KEY_RSSI, parser.nextInt());
        position.setAltitude(parser.nextDouble(0));
        position.set(Position.KEY_HDOP, parser.nextDouble());

        return position;
    }

}
