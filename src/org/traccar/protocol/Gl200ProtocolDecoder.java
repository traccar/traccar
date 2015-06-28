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
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Gl200ProtocolDecoder extends BaseProtocolDecoder {

    public Gl200ProtocolDecoder(Gl200Protocol protocol) {
        super(protocol);
    }

    private static final Pattern heartbeatPattern = Pattern.compile(
            "\\+ACK\\:GTHBD," +
            "([0-9A-Z]{2}\\p{XDigit}{4})," +
            ".*," +
            "(\\p{XDigit}{4})\\$?");

    private static final Pattern pattern = Pattern.compile(
            "(?:(?:\\+(?:RESP|BUFF):)|" +
            "(?:\\x00?\\x04,\\p{XDigit}{4},[01],))" +
            "GT...," +
            "(?:[0-9A-Z]{2}\\p{XDigit}{4})?," + // Protocol version
            "([^,]+),.*," +                     // IMEI
            "(\\d*)," +                         // GPS accuracy
            "(\\d+.\\d)?," +                    // Speed
            "(\\d+)?," +                        // Course
            "(-?\\d+\\.\\d)?," +                // Altitude
            "(-?\\d+\\.\\d+)," +                // Longitude
            "(-?\\d+\\.\\d+)," +                // Latitude
            "(\\d{4})(\\d{2})(\\d{2})" +        // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
            "(\\d{4})?," +                      // MCC
            "(\\d{4})?," +                      // MNC
            "(\\p{XDigit}{4}|\\p{XDigit}{8})?," + // LAC
            "(\\p{XDigit}{4})?," +              // Cell
            "(?:(\\d+\\.\\d)?," +               // Odometer
            "(\\d{1,3})?,)?" +                  // Battery
            ".*," +
            "(\\p{XDigit}{4})\\$?");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Handle heartbeat
        Matcher parser = heartbeatPattern.matcher(sentence);
        if (parser.matches()) {
            if (channel != null) {
                channel.write("+SACK:GTHBD," + parser.group(1) + "," + parser.group(2) + "$", remoteAddress);
            }
            return null;
        }

        // Parse message
        parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());

        Integer index = 1;

        // Get device by IMEI
        if (!identify(parser.group(index++), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Validity
        position.setValid(Integer.valueOf(parser.group(index++)) < 20);

        // Speed
        String speed = parser.group(index++);
        if (speed != null) {
            position.setSpeed(UnitsConverter.knotsFromKph(Double.valueOf(speed)));
        }

        // Course
        String course = parser.group(index++);
        if (speed != null) {
            position.setCourse(Double.valueOf(course));
        }

        // Altitude
        String altitude = parser.group(index++);
        if (speed != null) {
            position.setAltitude(Double.valueOf(altitude));
        }

        // Coordinates
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setLatitude(Double.valueOf(parser.group(index++)));

        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));

        // Time
        time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());

        // Cell information
        position.set(Event.KEY_MCC, parser.group(index++));
        position.set(Event.KEY_MNC, parser.group(index++));
        position.set(Event.KEY_LAC, parser.group(index++));
        position.set(Event.KEY_CELL, parser.group(index++));

        // Other
        String odometer = parser.group(index++);
        if (odometer != null && Double.valueOf(odometer) != 0) {
            position.set(Event.KEY_ODOMETER, odometer);
        }
        position.set(Event.KEY_BATTERY, parser.group(index++));

        if (Boolean.valueOf(Context.getProps().getProperty(getProtocolName() + ".ack")) && channel != null) {
            channel.write("+SACK:" + parser.group(index++) + "$", remoteAddress);
        }

        return position;
    }

}
