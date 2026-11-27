/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class R16hProtocolDecoder extends BaseProtocolDecoder {

    public R16hProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPSD = new PatternBuilder()
            .text("@GPSD,")
            .number("(d+),")                     // imei
            .expression("([RS]),")               // real time / history
            .number("(dddd)(dd)(dd),")           // date
            .number("(dd)(dd)(dd),")             // time
            .number("(d+.d+),")                  // latitude
            .expression("([NS]),")
            .number("(d+.d+),")                  // longitude
            .expression("([EW]),")
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // battery
            .expression("([LR]),")               // strap locking status
            .expression("(\\w*)")                // alarm flag
            .compile();

    private static final Pattern PATTERN_LBSD = new PatternBuilder()
            .text("@LBSD,")
            .number("(d+),")                     // imei
            .expression("([RS]),")               // real time / history
            .number("(dddd)(dd)(dd),")           // date
            .number("(dd)(dd)(dd),")             // time
            .number("(d+)-")                     // mcc
            .number("(d+)-")                     // mnc
            .number("(x+)-")                     // lac
            .number("(x+):")                     // cid
            .number("(-?d+),,,")                 // rssi
            .number("(d+),")                     // battery
            .expression("([LR]),")               // strap locking status
            .expression("(\\w*)")                // alarm flag
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = ((String) msg).trim();

        if (sentence.startsWith("@LINK,")) {
            getDeviceSession(channel, remoteAddress, sentence.substring("@LINK,".length()));
            return null;
        } else if (sentence.startsWith("@LBSD,")) {
            return decodeLbsd(channel, remoteAddress, sentence);
        } else {
            return decodeGpsd(channel, remoteAddress, sentence);
        }
    }

    private Position decodeGpsd(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_GPSD, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = initPosition(channel, remoteAddress, parser);
        if (position == null) {
            return null;
        }

        position.setTime(parser.nextDateTime());
        position.setValid(true);
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        decodeStatus(position, parser);

        return position;
    }

    private Position decodeLbsd(Channel channel, SocketAddress remoteAddress, String sentence) {
        Parser parser = new Parser(PATTERN_LBSD, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = initPosition(channel, remoteAddress, parser);
        if (position == null) {
            return null;
        }

        getLastLocation(position, parser.nextDateTime());

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt(), parser.nextInt())));

        decodeStatus(position, parser);

        return position;
    }

    private Position initPosition(Channel channel, SocketAddress remoteAddress, Parser parser) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (parser.next().equals("S")) {
            position.set(Position.KEY_ARCHIVE, true);
        }

        return position;
    }

    private void decodeStatus(Position position, Parser parser) {
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set("strapLocked", parser.next().equals("L"));

        switch (parser.next()) {
            case "SOS" -> position.addAlarm(Position.ALARM_SOS);
            case "LBT" -> position.addAlarm(Position.ALARM_LOW_BATTERY);
            case "ULK" -> position.addAlarm(Position.ALARM_REMOVING);
            default -> {}
        }
    }

}
