/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.PatternBuilder;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.traccar.DeviceSession;
import org.traccar.helper.Parser;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class SviasProtocolDecoder extends BaseProtocolDecoder {

    public SviasProtocolDecoder(SviasProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("[") // delimiter init
            .any()
            .number("(dddddddd),") // imei
            .any()
            .number("(d+),") // date (yyyymmdd)
            .number("(d+),") // time (hhmmss)
            .number("(-?d+),") // longitude
            .number("(-?d+),") // latitude
            .number("(d+),") // speed
            .number("(d+),") // course
            .number("(d+),") // odometer
            .number("(d+),") // input
            .number("(d+),") // output / status
            .any()
            .number("(ddddd),") // main power voltage
            .number("(d+),") // percentual power internal battery
            .number("(d+),") // RSSID
            .any()
            .compile();

    private double convertCoordinates(long v) {
        return Double.valueOf(((float) ((((float) v / 1.0E7F)
                - ((int) (v / 10000000L))) * 1.6666666666666667D)) + ((int) (v / 10000000L)));
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;
        Object result = null;

        if (!sentence.contains(":")) {

            Parser parser = new Parser(PATTERN, (String) sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }

            position.setDeviceId(deviceSession.getDeviceId());

            DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            String date = String.format("%06d", parser.nextInt());
            String time = String.format("%06d", parser.nextInt());

            position.setTime(dateFormat.parse(date + time));

            position.setLatitude(convertCoordinates(parser.nextLong()));
            position.setLongitude(convertCoordinates(parser.nextLong()));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0) / 100));
            position.setCourse(parser.nextDouble(0) / 100);
            position.setAltitude(0);

            position.set(Position.KEY_ODOMETER, parser.nextInt());

            String input = new StringBuilder(String.format("%08d",
                    Integer.parseInt(Integer.toString(parser.nextInt(), 2)))).reverse().toString();

            String output = new StringBuilder(String.format("%08d",
                    Integer.parseInt(Integer.toString(parser.nextInt(), 2)))).reverse().toString();

            position.set(Position.KEY_ALARM, (input.substring(0, 1).equals("1")
                    ? Position.ALARM_SOS : null));

            position.set(Position.KEY_IGNITION, input.substring(4, 5).equals("1"));

            position.setValid(output.substring(0, 1).equals("1"));

            position.set(Position.KEY_POWER, parser.nextDouble(0) / 1000);

            position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());

            position.set(Position.KEY_RSSI, parser.nextInt());

            result = position;

        }

        if (channel != null) {
            channel.write("@");
        }

        return result;

    }

}
