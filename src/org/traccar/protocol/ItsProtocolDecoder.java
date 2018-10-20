/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class ItsProtocolDecoder extends BaseProtocolDecoder {

    public ItsProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$,")
            .expression("[^,]+,")                // event
            .groupBegin()
            .expression("[^,]+,")                // vendor
            .number("d+.d+.d+,")                 // firmware version
            .groupEnd("?")
            .expression("[^,]+,")                // type
            .groupBegin()
            .number("d+,")
            .expression("[LH],")                 // history
            .groupEnd("?")
            .number("(d{15}),")                  // imei
            .groupBegin()
            .expression("(?:NM|SP),")            // status
            .or()
            .expression("[^,]+,")                // vehicle registration
            .number("([01]),")                   // valid
            .groupEnd()
            .number("(dd),(dd),(dddd),")         // date (ddmmyyyy)
            .number("(dd),(dd),(dd),")           // time (hhmmss)
            .expression("([AV]),").optional()    // valid
            .number("(d+.d+),([NS]),")           // latitude
            .number("(d+.d+),([EW]),")           // longitude
            .number("(-?d+.d+),").optional()     // altitude
            .number("(d+.d+),")                  // speed
            .any()
            .compile();

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

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.hasNext()) {
            position.setValid(parser.nextInt() == 1);
        }
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        if (parser.hasNext()) {
            position.setValid(parser.next().equals("A"));
        }
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setAltitude(parser.nextDouble(0));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        return position;
    }

}
