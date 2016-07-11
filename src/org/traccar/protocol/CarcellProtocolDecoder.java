/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.Parser.CoordinateFormat;
import org.traccar.model.Position;

public class CarcellProtocolDecoder extends BaseProtocolDecoder {

    public CarcellProtocolDecoder(CarcellProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPS = new PatternBuilder()
            .expression("([$%])")                // memory flag
            .number("(d+),")                     // imei
            .number("([NS])(dd)(dd).(dddd),")    // latitude
            .number("([EW])(ddd)(dd).(dddd),")   // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("([-+]ddd)([-+]ddd)([-+]ddd),")       // x,y,z
            .number("(d+),")                     // battery
            .number("(d+),")                     // csq
            .number("(d),")                      // jamming
            .number("(d+),")                     // hdop
            .expression("([CG])")                // clock type
            .number("(dd)(dd)(dd),")             // date
            .number("(dd)(dd)(dd),")             // time
            .number("(d),")                      // block
            .number("(d),")                      // ignition
            .number("(d),")                      // cloned
            .expression("([AF])")                // panic
            .number("(d),")                      // painel
            .number("(d+),")                     // battery voltage
            .any()                               // full format
            .compile();
    
    private static final Pattern PATTERN_CEL = new PatternBuilder()
            .expression("([$%])")                // memory flag
            .number("(d+),")                     // imei
            .text("CEL,")
            .number("([NS])(d+.d+),")            // latitude
            .number("([EW])(d+.d+),")            // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("([-+]ddd)([-+]ddd)([-+]ddd),")       // x,y,z
            .number("(d+),")                     // battery
            .number("(d+),")                     // csq
            .number("(d),")                      // jamming
            .number("(d+),")                     // hdop
            .expression("([CG])")                // clock type
            .number("(dd)(dd)(dd),")             // date
            .number("(dd)(dd)(dd),")             // time
            .number("(d),")                      // block
            .number("(d),")                      // ignition
            .number("(d),")                      // cloned
            .expression("([AF])")                // panic
            .number("(d),")                      // painel
            .number("(d+),")                     // battery voltage
            .any()                               // full format
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Pattern pattern = null;
        String sentence = (String) msg;
        
        if (sentence.indexOf("CEL,") < 0) {
            pattern = PATTERN_GPS;
        } else {
            pattern = PATTERN_CEL;
        }
        
        Parser parser = new Parser(pattern, (String) msg);
        
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.set(Position.KEY_ARCHIVE, parser.next().equals("%"));
        position.setValid(true);
        
        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        
        position.setDeviceId(getDeviceId());
        
        if (PATTERN_GPS == pattern) {
            position.setLatitude(parser.nextCoordinate(CoordinateFormat.HEM_DEG_MIN_MIN));
            position.setLongitude(parser.nextCoordinate(CoordinateFormat.HEM_DEG_MIN_MIN));
        } else {
            position.setLatitude(parser.nextCoordinate(CoordinateFormat.HEM_DEG));
            position.setLongitude(parser.nextCoordinate(CoordinateFormat.HEM_DEG));
        }
        
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.set("x", parser.nextInt());
        position.set("y", parser.nextInt());
        position.set("z", parser.nextInt());
        
        Double internalBattery = (parser.nextDouble() + 100d) * 0.0294d;
        position.set(Position.KEY_BATTERY, internalBattery);
        position.set(Position.KEY_GSM, parser.nextInt());
        position.set("jamming", parser.next().equals("1"));
        position.set(Position.KEY_GPS, parser.nextInt());
        
        parser.next(); // clock type
        
        DateBuilder dateBuilder = new DateBuilder().
                setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());
        
        position.set("blocked", parser.next().equals("1"));
        position.set(Position.KEY_IGNITION, parser.next().equals("1"));
        position.set("cloned", parser.next().equals("1"));
        
        parser.next(); // panic button status
        
        Integer painelStatus = parser.nextInt();
        position.set(Position.KEY_ALARM, painelStatus.equals("1"));
        position.set("painel", painelStatus.equals("2"));
        
        Double mainVoltage = parser.nextDouble() / 100d;
        position.set(Position.KEY_POWER, mainVoltage);

        return position;
    }

}
