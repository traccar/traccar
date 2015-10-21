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
            .txt("+ACK:GTHBD,")
            .num("([0-9A-Z]{2}xxxx),")
            .any().txt(",")
            .num("(xxxx)")
            .opt("$")
            .compile();

    private static final Pattern PATTERN = new PatternBuilder()
            .groupBegin()
            .txt("+").grp("RESP|BUFF").txt(":")
            .or()
            .bin("00?04,")
            .num("xxxx,")
            .xpr("[01],")
            .groupEnd(false)
            .xpr("GT...,")
            .opn("[0-9A-Z]{2}xxxx").txt(",")     // protocol version
            .xpr("([^,]+),")                     // imei

            .groupBegin()
            .xpr("[0-9A-Z]{17},")                // vin
            .xpr("[^,]{0,20},")                  // device name
            .xpr("[01],")                        // report type
            .num("x{1,8},")                      // report mask
            .xpr("[0-9A-Z]{17},")                // vin
            .num("[01],")                        // obd connect
            .num("d{1,5},")                      // obd voltage
            .num("x{8},")                        // support pids
            .num("(d{1,5}),")                    // engine rpm
            .num("(d{1,3}),")                    // speed
            .num("(-?d{1,3}),")                  // coolant temp
            .num("(d+.?d*|Inf|NaN)?,")           // fuel consumption
            .num("(d{1,5}),")                    // dtcs cleared distance
            .num("d{1,5},")
            .xpr("([01]),")                      // obd connect
            .num("(d{1,3}),")                    // number of dtcs
            .num("(x*),")                        // dtcs
            .num("(d{1,3}),")                    // throttle
            .num("d{1,3},")                      // engine load
            .num("(d{1,3})?,")                   // fuel level
            .num("(d+)")                         // odometer
            .or().any()
            .groupEnd(false).txt(",")

            .num("(d*),")                        // gps accuracy
            .num("(d+.d)?,")                     // speed
            .num("(d+)?,")                       // course
            .num("(-?d+.d)?,")                   // altitude
            .num("(-?d+.d+),")                   // longitude
            .num("(-?d+.d+),")                   // latitude
            .num("(dddd)(dd)(dd)")               // date
            .num("(dd)(dd)(dd),")                // time
            .num("(dddd)?,")                     // mcc
            .num("(dddd)?,")                     // mnc
            .num("(xxxx|x{8})?,")                // loc
            .num("(xxxx)?,")                     // cell
            .groupBegin()
            .num("(d+.d)?,")                     // odometer
            .num("(d{1,3})?,")                   // battery
            .groupEnd(true)
            .any().txt(",")
            .num("(xxxx)\\$?")
            .opt("$")
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN_HEARTBEAT, (String) msg);
        if (parser.matches()) {
            if (channel != null) {
                channel.write("+SACK:GTHBD," + parser.next() + "," + parser.next() + "$", remoteAddress);
            }
            return null;
        }

        parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // OBD
        position.set(Event.KEY_RPM, parser.next());
        position.set(Event.KEY_OBD_SPEED, parser.next());
        position.set(Event.PREFIX_TEMP + 1, parser.next());
        position.set("fuel-consumption", parser.next());
        position.set("dtcs-cleared-distance", parser.next());
        position.set("odb-connect", parser.next());
        position.set("dtcs-number", parser.next());
        position.set("dtcs-codes", parser.next());
        position.set("throttle-position", parser.next());
        position.set(Event.KEY_FUEL, parser.next());
        position.set(Event.KEY_OBD_ODOMETER, parser.next());

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

        position.set(Event.KEY_MCC, parser.next());
        position.set(Event.KEY_MNC, parser.next());
        position.set(Event.KEY_LAC, parser.next());
        position.set(Event.KEY_CELL, parser.next());

        position.set(Event.KEY_ODOMETER, parser.next());
        position.set(Event.KEY_BATTERY, parser.next());

        if (Context.getConfig().getBoolean(getProtocolName() + ".ack") && channel != null) {
            channel.write("+SACK:" + parser.next() + "$", remoteAddress);
        }

        return position;
    }

}
