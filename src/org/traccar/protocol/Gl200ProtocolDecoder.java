/*
 * Copyright 2012 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Gl200ProtocolDecoder extends BaseProtocolDecoder {

    public Gl200ProtocolDecoder(Gl200Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_HEARTBEAT = new PatternBuilder()
            .text("+ACK:GTHBD,")
            .number("([0-9A-Z]{2}xxxx),")
            .any().text(",")
            .number("(xxxx)")
            .text("$").optional()
            .compile();

    private static final Pattern PATTERN_INF = new PatternBuilder()
            .text("+RESP:GTINF,")
            .number("[0-9A-Z]{2}xxxx,")          // protocol version
            .number("(d{15}),")                  // imei
            .expression("[0-9A-Z]{17},")         // vin
            .expression("[^,]{0,20},")           // device name
            .number("(xx),")                     // state
            .expression("[0-9F]{20},")           // iccid
            .number("d{1,2},")
            .number("d{1,2},")
            .expression("[01],")
            .number("(d{1,5}),")                 // power
            .text(",")
            .number("(d+.d+),")                  // battery
            .expression("([01]),")               // charging
            .expression("[01],")
            .text(",,")
            .number("d{14},")                    // last fix time
            .text(",,,,,")
            .number("[-+]dddd,")                 // timezone
            .expression("[01],")                 // daylight saving
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd),")             // time
            .number("(xxxx)")                    // counter
            .text("$").optional()
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .groupBegin()
            .text("+").expression("(?:RESP|BUFF)").text(":")
            .or()
            .binary("00?04,")
            .number("xxxx,")
            .expression("[01],")
            .groupEnd()
            .expression("GT...,")
            .number("(?:[0-9A-Z]{2}xxxx)?,")     // protocol version
            .expression("([^,]+),")              // imei

            .groupBegin()
            .expression("[0-9A-Z]{17},")         // vin
            .expression("[^,]{0,20},")           // device name
            .expression("[01],")                 // report type
            .number("x{1,8},")                   // report mask
            .expression("[0-9A-Z]{17},")         // vin
            .number("[01],")                     // obd connect
            .number("d{1,5},")                   // obd voltage
            .number("x{8},")                     // support pids
            .number("(d{1,5}),")                 // engine rpm
            .number("(d{1,3}),")                 // speed
            .number("(-?d{1,3}),")               // coolant temp
            .number("(d+.?d*|Inf|NaN)?,")        // fuel consumption
            .number("(d{1,5}),")                 // dtcs cleared distance
            .number("d{1,5},")
            .expression("([01]),")               // obd connect
            .number("(d{1,3}),")                 // number of dtcs
            .number("(x*),")                     // dtcs
            .number("(d{1,3}),")                 // throttle
            .number("d{1,3},")                   // engine load
            .number("(d{1,3})?,")                // fuel level
            .number("(d+)")                      // odometer
            .or().any()
            .groupEnd().text(",")

            .number("(d{1,2})?,")                // gps accuracy
            .number("(d{1,3}.d)?,")              // speed
            .number("(d{1,3})?,")                // course
            .number("(-?d{1,5}.d)?,")            // altitude
            .number("(-?d{1,3}.d{6})?,")         // longitude
            .number("(-?d{1,2}.d{6})?,")         // latitude
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd)").optional(2)  // time
            .text(",")
            .number("(0ddd)?,")                  // mcc
            .number("(0ddd)?,")                  // mnc
            .number("(?:xxxx)?")
            .number("(xxxx)").optional(2)        // lac
            .text(",")
            .number("(xxxx)?,")                  // cell
            .groupBegin()
            .number("(d+.d)?,")                  // odometer
            .number("(d{1,3})?,")                // battery
            .groupEnd("?")
            .groupBegin()
            .any()
            .number("(dddd)(dd)(dd)")            // date
            .number("(dd)(dd)(dd)")              // time
            .or()
            .any()
            .groupEnd()
            .number(",(xxxx)")
            .text("$").optional()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        Parser parser = new Parser(PATTERN_HEARTBEAT, sentence);
        if (parser.matches()) {
            if (channel != null) {
                channel.write("+SACK:GTHBD," + parser.next() + "," + parser.next() + "$", remoteAddress);
            }
            return null;
        }

        parser = new Parser(PATTERN_INF, sentence);
        if (parser.matches()) {

            Position position = new Position();
            position.setProtocol(getProtocolName());

            if (!identify(parser.next(), channel, remoteAddress)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            position.set(Event.KEY_STATUS, parser.next());
            position.set(Event.KEY_POWER, parser.next());
            position.set(Event.KEY_BATTERY, parser.next());
            position.set(Event.KEY_CHARGE, parser.next());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            getLastLocation(position, dateBuilder.getDate());

            position.set(Event.KEY_INDEX, parser.next());

            return position;
        }

        parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // RFID
        if (sentence.startsWith("+RESP:GTIDA")) {
            position.set(Event.KEY_RFID, sentence.split(",")[5]);
        }

        // OBD
        position.set(Event.KEY_RPM, parser.next());
        position.set(Event.KEY_OBD_SPEED, parser.next());
        position.set(Event.PREFIX_TEMP + 1, parser.next());
        position.set("fuel-consumption", parser.next());
        position.set("dtcs-cleared-distance", parser.next());
        position.set("odb-connect", parser.next());
        position.set("dtcs-number", parser.next());
        position.set("dtcs-codes", parser.next());
        position.set(Event.KEY_THROTTLE, parser.next());
        position.set(Event.KEY_FUEL, parser.next());
        position.set(Event.KEY_OBD_ODOMETER, parser.next());

        if (parser.hasNext(12)) {
            position.setValid(parser.nextInt() < 20);
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setCourse(parser.nextDouble());
            position.setAltitude(parser.nextDouble());
            position.setLongitude(parser.nextDouble());
            position.setLatitude(parser.nextDouble());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());
        } else {
            getLastLocation(position, null);
        }

        if (parser.hasNext(4)) {
            position.set(Event.KEY_MCC, parser.nextInt());
            position.set(Event.KEY_MNC, parser.nextInt());
            position.set(Event.KEY_LAC, parser.nextInt(16));
            position.set(Event.KEY_CID, parser.nextInt(16));
        }

        position.set(Event.KEY_ODOMETER, parser.next());
        position.set(Event.KEY_BATTERY, parser.next());

        if (parser.hasNext(6)) {
            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            if (!position.getOutdated() && position.getFixTime().after(dateBuilder.getDate())) {
                position.setTime(dateBuilder.getDate());
            }
        }

        if (Context.getConfig().getBoolean(getProtocolName() + ".ack") && channel != null) {
            channel.write("+SACK:" + parser.next() + "$", remoteAddress);
        }

        return position;
    }

}
