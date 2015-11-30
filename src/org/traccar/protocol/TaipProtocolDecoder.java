/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.Date;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class TaipProtocolDecoder extends BaseProtocolDecoder {

    private final boolean sendResponse;

    public TaipProtocolDecoder(TaipProtocol protocol, boolean sendResponse) {
        super(protocol);
        this.sendResponse = sendResponse;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .groupBegin()
            .expression("R[EP]V")                // type
            .groupBegin()
            .number("dd")                        // event index
            .number("(dddd)")                    // week
            .number("(d)")                       // day
            .groupEnd("?")
            .number("(d{5})")                    // seconds
            .or()
            .text("RGP")                         // type
            .number("(dd)(dd)(dd)")              // date
            .number("(dd)(dd)(dd)")              // time
            .groupEnd()
            .number("([-+]dd)(d{5})")            // latitude
            .number("([-+]ddd)(d{5})")           // longitude
            .number("(ddd)")                     // speed
            .number("(ddd)")                     // course
            .number("(d)")                       // fix mode
            .any()
            .compile();

    private Date getTime(long week, long day, long seconds) {
        DateBuilder dateBuilder = new DateBuilder()
                .setDate(1980, 1, 6)
                .addMillis(((week * 7 + day) * 24 * 60 * 60 + seconds) * 1000);
        return dateBuilder.getDate();
    }

    private Date getTime(long seconds) {
        DateBuilder dateBuilder = new DateBuilder(new Date())
                .setTime(0, 0, 0, 0)
                .addMillis(seconds * 1000);

        long millis = dateBuilder.getDate().getTime();
        long diff = System.currentTimeMillis() - millis;

        if (diff > 12 * 60 * 60 * 1000) {
            millis += 24 * 60 * 60 * 1000;
        } else if (diff < -12 * 60 * 60 * 1000) {
            millis -= 24 * 60 * 60 * 1000;
        }

        return new Date(millis);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        // Find message start
        int beginIndex = sentence.indexOf('>');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        // Find device identifier
        beginIndex = sentence.indexOf(";ID=");
        if (beginIndex != -1) {
            beginIndex += 4;
            int endIndex = sentence.indexOf(';', beginIndex);
            if (endIndex == -1) {
                endIndex = sentence.length();
            }

            String id = sentence.substring(beginIndex, endIndex);
            if (!identify(id, channel, remoteAddress)) {
                return null;
            }

            if (sendResponse && channel != null) {
                channel.write(id);
            }
        } else {
            return null;
        }

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        String week = parser.next();
        String day = parser.next();
        String seconds = parser.next();
        if (seconds != null) {
            if (week != null && day != null) {
                position.setTime(getTime(Integer.parseInt(week), Integer.parseInt(day), Integer.parseInt(seconds)));
            } else {
                position.setTime(getTime(Integer.parseInt(seconds)));
            }
        }

        if (parser.hasNext(6)) {
            DateBuilder dateBuilder = new DateBuilder()
                    .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());
        }

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_DEG));
        position.setSpeed(UnitsConverter.knotsFromMph(parser.nextDouble()));
        position.setCourse(parser.nextDouble());
        position.setValid(parser.nextInt() != 0);

        return position;
    }

}
