/*
 * Copyright 2013 - 2014 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class GlobalSatProtocolDecoder extends BaseProtocolDecoder {

    private String format0;
    private String format1;

    public GlobalSatProtocolDecoder(GlobalSatProtocol protocol) {
        super(protocol);

        format0 = Context.getConfig().getString(getProtocolName() + ".format0", "TSPRXAB27GHKLMnaicz*U!");
        format1 = Context.getConfig().getString(getProtocolName() + ".format1", "SARY*U!");
    }

    public void setFormat0(String format) {
        format0 = format;
    }

    public void setFormat1(String format) {
        format1 = format;
    }

    private Position decodeOriginal(Channel channel, SocketAddress remoteAddress, String sentence) {

        if (channel != null) {
            channel.write("ACK\r");
        }

        String format;
        if (sentence.startsWith("GSr")) {
            format = format0;
        } else if (sentence.startsWith("GSh")) {
            format = format1;
        } else {
            return null;
        }

        // Check that message contains required parameters
        if (!format.contains("B") || !format.contains("S") || !(format.contains("1")
                || format.contains("2") || format.contains("3")) || !(format.contains("6")
                || format.contains("7") || format.contains("8"))) {
            return null;
        }

        if (format.contains("*")) {
            format = format.substring(0, format.indexOf('*'));
            sentence = sentence.substring(0, sentence.indexOf('*'));
        }
        String[] values = sentence.split(",");

        Position position = new Position();
        position.setProtocol(getProtocolName());

        for (int formatIndex = 0, valueIndex = 1; formatIndex < format.length()
                && valueIndex < values.length; formatIndex++) {
            String value = values[valueIndex];

            switch (format.charAt(formatIndex)) {
                case 'S':
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, value);
                    if (deviceSession == null) {
                        return null;
                    }
                    position.setDeviceId(deviceSession.getDeviceId());
                    break;
                case 'A':
                    if (value.isEmpty()) {
                        position.setValid(false);
                    } else {
                        position.setValid(Integer.parseInt(value) != 1);
                    }
                    break;
                case 'B':
                    DateBuilder dateBuilder = new DateBuilder()
                            .setDay(Integer.parseInt(value.substring(0, 2)))
                            .setMonth(Integer.parseInt(value.substring(2, 4)))
                            .setYear(Integer.parseInt(value.substring(4)));
                    value = values[++valueIndex];
                    dateBuilder
                            .setHour(Integer.parseInt(value.substring(0, 2)))
                            .setMinute(Integer.parseInt(value.substring(2, 4)))
                            .setSecond(Integer.parseInt(value.substring(4)));
                    position.setTime(dateBuilder.getDate());
                    break;
                case 'C':
                    valueIndex += 1;
                    break;
                case '1':
                    double longitude = Double.parseDouble(value.substring(1));
                    if (value.charAt(0) == 'W') {
                        longitude = -longitude;
                    }
                    position.setLongitude(longitude);
                    break;
                case '2':
                    longitude = Double.parseDouble(value.substring(4)) / 60;
                    longitude += Integer.parseInt(value.substring(1, 4));
                    if (value.charAt(0) == 'W') {
                        longitude = -longitude;
                    }
                    position.setLongitude(longitude);
                    break;
                case '3':
                    position.setLongitude(Double.parseDouble(value) * 0.000001);
                    break;
                case '6':
                    double latitude = Double.parseDouble(value.substring(1));
                    if (value.charAt(0) == 'S') {
                        latitude = -latitude;
                    }
                    position.setLatitude(latitude);
                    break;
                case '7':
                    latitude = Double.parseDouble(value.substring(3)) / 60;
                    latitude += Integer.parseInt(value.substring(1, 3));
                    if (value.charAt(0) == 'S') {
                        latitude = -latitude;
                    }
                    position.setLatitude(latitude);
                    break;
                case '8':
                    position.setLatitude(Double.parseDouble(value) * 0.000001);
                    break;
                case 'G':
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case 'H':
                    position.setSpeed(Double.parseDouble(value));
                    break;
                case 'I':
                    position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(value)));
                    break;
                case 'J':
                    position.setSpeed(UnitsConverter.knotsFromMph(Double.parseDouble(value)));
                    break;
                case 'K':
                    position.setCourse(Double.parseDouble(value));
                    break;
                case 'N':
                    position.set(Position.KEY_BATTERY, value);
                    break;
                default:
                    // Unsupported
                    break;
            }

            valueIndex += 1;
        }
        return position;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$")
            .number("(d+),")                     // imei
            .number("d+,")                       // mode
            .number("(d+),")                     // fix
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([EW])")
            .number("(ddd)(dd.d+),")             // longitude (dddmm.mmmm)
            .expression("([NS])")
            .number("(dd)(dd.d+),")              // latitude (ddmm.mmmm)
            .number("(d+.?d*),")                 // altitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*)?,")                // course
            .number("(d+)[,*]")                  // satellites
            .number("(d+.?d*)")                  // hdop
            .compile();

    private Position decodeAlternative(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
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

        position.setValid(!parser.next().equals("1"));
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG_MIN));
        position.setAltitude(parser.nextDouble());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.next());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("GS")) {
            return decodeOriginal(channel, remoteAddress, sentence);
        } else if (sentence.startsWith("$")) {
            return decodeAlternative(channel, remoteAddress, sentence);
        }

        return null;
    }

}
