/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class Tk103ProtocolDecoder extends BaseProtocolDecoder {

    public Tk103ProtocolDecoder(Tk103Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+)(,)?")                  // device id
            .expression(".{4},?")                // command
            .number("d*")                        // imei?
            .number("(dd)(dd)(dd),?")            // date (mmddyy if comma-delimited, otherwise yyddmm)
            .expression("([AV]),?")              // validity
            .number("(d+)(dd.d+)")               // latitude
            .expression("([NS]),?")
            .number("(d+)(dd.d+)")               // longitude
            .expression("([EW]),?")
            .number("(d+.d)(?:d*,)?")            // speed
            .number("(dd)(dd)(dd),?")            // time (hhmmss)
            .number("(d+.?d{1,2}),?")            // course
            .number("(?:([01]{8})|(x{8}))?,?")   // state
            .number("(?:L(x+))?")                // odometer
            .any()
            .number("([+-]ddd.d)?")              // temperature
            .text(")").optional()
            .compile();

    private static final Pattern PATTERN_BATTERY = new PatternBuilder()
            .number("(d+),")                     // device id
            .text("ZC20,")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("d+,")                       // battery level
            .number("(d+),")                     // battery voltage
            .number("(d+),")                     // power voltage
            .number("d+")                        // installed
            .compile();

    private static final Pattern PATTERN_NETWORK = new PatternBuilder()
            .number("(d{12})")                   // device id
            .text("BZ00,")
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(x+),")                     // lac
            .number("(x+),")                     // cid
            .any()
            .compile();

    private String decodeAlarm(int value) {
        switch (value) {
            case 1:
                return Position.ALARM_ACCIDENT;
            case 2:
                return Position.ALARM_SOS;
            case 3:
                return Position.ALARM_VIBRATION;
            case 4:
                return Position.ALARM_LOW_SPEED;
            case 5:
                return Position.ALARM_OVERSPEED;
            case 6:
                return Position.ALARM_GEOFENCE_EXIT;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Find message start
        int beginIndex = sentence.indexOf('(');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        // Send response
        if (channel != null) {
            String id = sentence.substring(0, 12);
            String type = sentence.substring(12, 16);
            if (type.equals("BP00") || type.equals("BP05")) {
                String content = sentence.substring(16);
                if (content.length() >= 15) {
                    getDeviceSession(channel, remoteAddress, content.substring(0, 15));
                }
                if (type.equals("BP00")) {
                    channel.write("(" + id + "AP01HSO)");
                    return null;
                } else if (type.equals("BP05")) {
                    channel.write("(" + id + "AP05)");
                }
            }
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        Parser parser = new Parser(PATTERN_BATTERY, sentence);
        if (parser.matches()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

            int battery = parser.nextInt(0);
            if (battery != 65535) {
                position.set(Position.KEY_BATTERY, battery * 0.01);
            }

            int power = parser.nextInt(0);
            if (power != 65535) {
                position.set(Position.KEY_POWER, power * 0.1);
            }

            return position;
        }

        parser = new Parser(PATTERN_NETWORK, sentence);
        if (parser.matches()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.setNetwork(new Network(CellTower.from(
                    parser.nextInt(0), parser.nextInt(0), parser.nextHexInt(0), parser.nextHexInt(0))));

            return position;
        }

        parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int alarm = sentence.indexOf("BO01");
        if (alarm != -1) {
            position.set(Position.KEY_ALARM, decodeAlarm(Integer.parseInt(sentence.substring(alarm + 4, alarm + 5))));
        }

        DateBuilder dateBuilder = new DateBuilder();
        if (parser.next() == null) {
            dateBuilder.setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        } else {
            dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        }

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        switch (Context.getConfig().getString(getProtocolName() + ".speed", "kmh")) {
            case "kn":
                position.setSpeed(parser.nextDouble(0));
                break;
            case "mph":
                position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble(0)));
                break;
            default:
                position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
                break;
        }

        dateBuilder.setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.setCourse(parser.nextDouble(0));

        String status = parser.next();
        if (status != null) {
            position.set(Position.KEY_STATUS, status); // binary status

            int value = Integer.parseInt(new StringBuilder(status).reverse().toString(), 2);
            position.set(Position.KEY_CHARGE, !BitUtil.check(value, 0));
            position.set(Position.KEY_IGNITION, BitUtil.check(value, 1));
        }
        position.set(Position.KEY_STATUS, parser.next()); // hex status

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextLong(16, 0));
        }

        if (parser.hasNext()) {
            position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
        }

        return position;
    }

}
