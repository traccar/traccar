/*
 * Copyright 2021 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class StartekProtocolDecoder extends BaseProtocolDecoder {

    public StartekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("&&")
            .expression(".")                     // index
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("(xxx),")                    // type
            .expression("(.+)")                  // content
            .number("xx")                        // checksum
            .text("\r\n")
            .compile();

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
            .number("(d+),")                     // event
            .expression("([^,]+)?,")             // event data
            .number("(dd)(dd)(dd)")              // date (yyymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AV]),")               // valid
            .number("(-?d+.d+),")                // longitude
            .number("(-?d+.d+),")                // latitude
            .number("(d+),")                     // satellites
            .number("(d+.d+),")                  // hdop
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cid
            .number("(d+),")                     // rssi
            .number("(x+),")                     // status
            .number("(x+),")                     // inputs
            .number("(x+),")                     // outputs
            .number("(x+)|")                     // power
            .number("(x+)")                      // battery
            .expression("([^,]+)?")              // adc
            .groupBegin()
            .number(",d+")                       // extended
            .expression(",([^,]+)?")             // fuel
            .groupBegin()
            .expression(",([^,]+)?")             // temperature
            .groupBegin()
            .text(",")
            .groupBegin()
            .number("(d+)?|")                    // rpm
            .number("(d+)?|")                    // engine load
            .number("(d+)?|")                    // maf flow
            .number("(d+)?|")                    // intake pressure
            .number("(d+)?|")                    // intake temperature
            .number("(d+)?|")                    // throttle
            .number("(d+)?|")                    // coolant temperature
            .number("(d+)?|")                    // instant fuel
            .number("(d+)[%L]").optional()       // fuel level
            .groupEnd("?")
            .number(",(d+)").optional()          // hours
            .groupEnd("?")
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_SOS;
            case 5, 6 -> Position.ALARM_DOOR;
            case 17 -> Position.ALARM_LOW_POWER;
            case 18 -> Position.ALARM_POWER_CUT;
            case 19 -> Position.ALARM_POWER_RESTORED;
            case 39 -> Position.ALARM_ACCELERATION;
            case 40 -> Position.ALARM_BRAKING;
            case 41 -> Position.ALARM_CORNERING;
            default -> null;
        };
    }

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

        String type = parser.next();
        String content = parser.next();
        return switch (type) {
            case "000" -> decodePosition(deviceSession, content);
            case "710" -> decodeSerial(deviceSession, content);
            default -> {
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());
                getLastLocation(position, null);
                position.set(Position.KEY_TYPE, type);
                position.set(Position.KEY_RESULT, content);
                yield position;
            }
        };
    }

    private Object decodePosition(DeviceSession deviceSession, String content) {

        Parser parser = new Parser(PATTERN_POSITION, content);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int event = parser.nextInt();
        String eventData = parser.next();
        position.set(Position.KEY_EVENT, event);
        if (event == 53) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, eventData);
        } else {
            position.addAlarm(decodeAlarm(event));
        }

        position.setTime(parser.nextDateTime());
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextDouble());

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        position.set(Position.KEY_ODOMETER, parser.nextLong());

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt(), parser.nextInt())));

        position.set(Position.KEY_STATUS, parser.nextHexInt());

        int input = parser.nextHexInt();
        int output = parser.nextHexInt();
        position.set(Position.KEY_IGNITION, BitUtil.check(input, 1));
        position.set(Position.KEY_DOOR, BitUtil.check(input, 2));
        position.set(Position.KEY_INPUT, input);
        position.set(Position.KEY_OUTPUT, output);

        position.set(Position.KEY_POWER, parser.nextHexInt() * 0.01);
        position.set(Position.KEY_BATTERY, parser.nextHexInt() * 0.01);

        if (parser.hasNext()) {
            String[] adc = parser.next().split("\\|");
            for (int i = 1; i < adc.length; i++) {
                position.set(Position.PREFIX_ADC + (i + 1), Integer.parseInt(adc[i], 16) * 0.01);
            }
        }

        if (parser.hasNext()) {
            String[] fuels = parser.next().split("\\|");
            for (String fuel : fuels) {
                int index = Integer.parseInt(fuel.substring(0, 2));
                int value = Integer.parseInt(fuel.substring(2), 16);
                position.set("fuel" + index, value * 0.1);
            }
        }

        if (parser.hasNext()) {
            String[] temperatures = parser.next().split("\\|");
            for (String temperature : temperatures) {
                int index = Integer.parseInt(temperature.substring(0, 2));
                int value = Integer.parseInt(temperature.substring(2), 16);
                double convertedValue = BitUtil.to(value, 15);
                if (BitUtil.check(value, 15)) {
                    convertedValue = -convertedValue;
                }
                position.set(Position.PREFIX_TEMP + index, convertedValue * 0.1);
            }
        }

        if (parser.hasNextAny(9)) {
            position.set(Position.KEY_RPM, parser.nextInt());
            position.set(Position.KEY_ENGINE_LOAD, parser.nextInt());
            position.set("airFlow", parser.nextInt());
            position.set("airPressure", parser.nextInt());
            if (parser.hasNext()) {
                position.set("airTemp", parser.nextInt() - 40);
            }
            position.set(Position.KEY_THROTTLE, parser.nextInt());
            if (parser.hasNext()) {
                position.set(Position.KEY_COOLANT_TEMP, parser.nextInt() - 40);
            }
            if (parser.hasNext()) {
                position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextInt() * 0.1);
            }
            position.set(Position.KEY_FUEL_LEVEL, parser.nextInt());
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_HOURS, parser.nextInt() * 1000L);
        }

        return position;
    }

    private Object decodeSerial(DeviceSession deviceSession, String content) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        String[] frames = content.split("\r\n");

        for (String frame : frames) {
            String[] values = frame.split(",");
            int index = 0;
            String type = values[index++];
            switch (type) {
                case "T1" -> {
                    index += 1; // speed
                    position.set(Position.KEY_RPM, Double.parseDouble(values[index++]));
                    index += 1; // fuel consumption
                    position.set(Position.KEY_FUEL_LEVEL, Double.parseDouble(values[index++]));
                    index += 4; // axel weights
                    index += 1; // turbo pressure
                    position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(values[index++]));
                    index += 1; // accelerator pedal
                    position.set("torque", Integer.parseInt(values[index++]));
                    index += 1; // firmware version
                    position.set(Position.KEY_POWER, Double.parseDouble(values[index++]));
                    index += 1; // coolant level
                    position.set("oilTemp", Double.parseDouble(values[index++]));
                    index += 1; // oil level
                    position.set(Position.KEY_THROTTLE, Double.parseDouble(values[index++]));
                    index += 1; // air inlet pressure
                    index += 1; // fuel tank secondary
                    index += 1; // current gear
                    index += 1; // seatbelt
                    position.set("oilPressure", Integer.parseInt(values[index++]));
                    index += 1; // wet tank air pressure
                    index += 1; // pto state
                    int ignition = Integer.parseInt(values[index++]);
                    if (ignition < 2) {
                        position.set(Position.KEY_IGNITION, ignition > 0);
                    }
                    index += 1; // brake pedal
                    position.set("catalystLevel", Double.parseDouble(values[index++]));
                    index += 1; // fuel type
                }
                case "T2" -> {
                    position.set(Position.KEY_ODOMETER, Double.parseDouble(values[index++]) * 1000);
                    index += 1; // total fuel
                    index += 1; // fuel used cruise
                    index += 1; // fuel used drive
                    index += 1;
                    index += 1;
                    index += 1; // total idle time
                    index += 1; // total time pto
                    index += 1; // time cruise
                    index += 1;
                    index += 1;
                    index += 1;
                    index += 1;
                    index += 1;
                    index += 1; // brake apps
                    index += 1; // clutch apps
                    position.set(Position.KEY_HOURS, Integer.parseInt(values[index++]));
                    index += 1; // time torque
                    position.set(Position.KEY_FUEL_CONSUMPTION, Double.parseDouble(values[index++]));
                    index += 1; // total cruise control distance
                    position.set(Position.KEY_FUEL_USED, Double.parseDouble(values[index++]));
                    index += 1; // total drive time
                }
            }
        }

        return position;
    }

}
