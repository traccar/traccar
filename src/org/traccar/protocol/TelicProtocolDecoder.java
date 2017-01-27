/*
 * Copyright 2014 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Log;

public class TelicProtocolDecoder extends BaseProtocolDecoder {

    public TelicProtocolDecoder(TelicProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("dddd")
            .number("(d{6})") // device id
            .number("(d+),") // type
            .number("d{12},") // event time
            .number("d+,")
            .number("(dd)(dd)(dd)") // date
            .number("(dd)(dd)(dd),") // time
            .groupBegin()
            .number("(ddd)(dd)(dddd),") // longitude
            .number("(dd)(dd)(dddd),") // latitude
            .or()
            .number("(-?d+),") // longitude
            .number("(-?d+),") // latitude
            .groupEnd()
            .number("(d),") // validity
            .number("(d+),") // speed
            .number("(d+),") // course
            .number("(d+),") // satellites
            .expression("(?:[^,]*,){7}")
            .number("(d+),") // battery
            .expression("[^,]*,")
            .number("(d+),") // external
            .any()
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

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        String eventValue = parser.next();
        position.set(Position.KEY_TYPE, eventValue);

        // alarm
        position.set(Position.KEY_ALARM, decodeAlarm(eventValue));

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        if (parser.hasNext(6)) {
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN));
        }

        if (parser.hasNext(2)) {
            position.setLongitude(parser.nextDouble() / 10000);
            position.setLatitude(parser.nextDouble() / 10000);
        }

        position.setValid(parser.nextInt() != 1);
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.next());
        position.set(Position.KEY_BATTERY, 3.4 + parser.nextInt() * 0.00345);
        position.set(Position.KEY_POWER, 6.0 + parser.nextInt() * 0.125);

        return position;
    }

    private String decodeAlarm(String event) {

        int eventId = 0;

        try {
            eventId = Integer.parseInt(event);
        } catch (Exception e) {
            Log.error("TelicProtocolDecoder: Error parsing event (" + eventId + "): " + e.toString());
        }

        switch (eventId) {
            case 1:
                return Position.ALARM_POWER_ON; // Power on
            case 2:
                return Position.ALARM_SOS; // Emergency
            case 3:
                return null; // Position lock alarm
            case 4:
                return null; // Alarm Tracking
            case 5:
                return Position.ALARM_POWER_OFF; // Power off
            case 6:
                return null; // Course change
            case 7:
                return Position.ALARM_GEOFENCE_ENTER; // Geofence area enter
            case 8:
                return Position.ALARM_GEOFENCE_EXIT; // Geofence area exit
            case 9:
                return null; // GPS fix lost
            case 10:
                return null; // Periodic wakeup / Routine message
            case 13:
                return null; // Digital input 2 Low -> High / Charger connected
            case 14:
                return null; // Digital input 2 High -> Low / Charger disconnected
            case 15:
                return null; // Digital input 3 Low -> High
            case 16:
                return null; // Digital input 3 High -> Low
            case 21:
                return null; // Analog input 1 High / Battery level OK
            case 22:
                return Position.ALARM_LOW_BATTERY; // Analog input 1 Low / Battery level low
            case 25:
                return Position.ALARM_MOVEMENT; // Device is moving
            case 26:
                return null; // Device is stationary
            case 29:
                return null; // Force GPRS reconnection
            default:
                return null;
        }
    }
}
