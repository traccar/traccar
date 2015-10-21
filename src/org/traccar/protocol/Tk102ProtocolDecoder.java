/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Position;

public class Tk102ProtocolDecoder extends BaseProtocolDecoder {

    public Tk102ProtocolDecoder(Tk102Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .txt("[")
            .xpr(".")
            .num("d{10}")
            .xpr(".")
            .txt("(")
            .xpr("[A-Z]+")
            .num("(dd)(dd)(dd)")     // Time (HHMMSS)
            .xpr("([AV])")                     // Validity
            .num("(dd)(dd.dddd)([NS])")  // Latitude (DDMM.MMMM)
            .num("(ddd)(dd.dddd)([EW])")  // Longitude (DDDMM.MMMM)
            .num("(ddd.ddd)")          // Speed
            .num("(dd)(dd)(dd)")   // Date (DDMMYY)
            .num("d+")
            .any()
            .txt(")")
            .opt("]")
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        if (sentence.startsWith("[!")) {

            if (!identify(sentence.substring(14, 14 + 15), channel)) {
                return null;
            }
            if (channel != null) {
                channel.write("[”0000000001" + sentence.substring(13) + "]");
            }

        } else if (hasDeviceId()) {

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(parser.nextDouble());

            dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            return position;
        }

        return null;
    }

}
