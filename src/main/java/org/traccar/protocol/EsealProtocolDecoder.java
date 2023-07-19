/*
 * Copyright 2018 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class EsealProtocolDecoder extends BaseProtocolDecoder {

    private String config;

    public EsealProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        config = getConfig().getString(Keys.PROTOCOL_CONFIG.withPrefix(getProtocolName()));
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("##S,")
            .expression("[^,]+,")                // device type
            .number("(d+),")                     // device id
            .number("d+,")                       // customer id
            .expression("[^,]+,")                // firmware version
            .expression("([^,]+),")              // type
            .number("(d+),")                     // index
            .number("(dddd)-(dd)-(dd),")         // date
            .number("(dd):(dd):(dd),")           // time
            .number("d+,")                       // interval
            .expression("([AV]),")               // validity
            .number("(d+.d+)([NS]) ")            // latitude
            .number("(d+.d+)([EW]),")            // longitude
            .number("(d+),")                     // course
            .number("(d+),")                     // speed
            .expression("([^,]+),")              // door
            .number("(d+.d+),")                  // acceleration
            .expression("([^,]+),")              // nfc
            .number("(d+.d+),")                  // battery
            .number("(-?d+),")                   // rssi
            .text("E##")
            .compile();

    private void sendResponse(Channel channel, String prefix, String type, String payload) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    prefix + type + "," + payload + ",E##\r\n", channel.remoteAddress()));
        }
    }

    private String decodeAlarm(String type) {
        switch (type) {
            case "Event-Door":
                return Position.ALARM_DOOR;
            case "Event-Shock":
                return Position.ALARM_VIBRATION;
            case "Event-Drop":
                return Position.ALARM_FALL_DOWN;
            case "Event-Lock":
                return Position.ALARM_LOCK;
            case "Event-RC-Unlock":
                return Position.ALARM_UNLOCK;
            default:
                return null;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String type = parser.next();
        String prefix = sentence.substring(0, sentence.indexOf(type));
        int index = parser.nextInt();

        position.set(Position.KEY_INDEX, index);
        position.set(Position.KEY_ALARM, decodeAlarm(type));

        switch (type) {
            case "Startup":
                sendResponse(channel, prefix, type + " ACK", index + "," + config);
                break;
            case "Normal":
            case "Button-Normal":
            case "Termination":
            case "Event-Door":
            case "Event-Shock":
            case "Event-Drop":
            case "Event-Lock":
            case "Event-RC-Unlock":
                sendResponse(channel, prefix, type + " ACK", String.valueOf(index));
                break;
            default:
                break;
        }

        position.setTime(parser.nextDateTime());
        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setCourse(parser.nextInt());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));

        switch (parser.next()) {
            case "Open":
                position.set(Position.KEY_DOOR, true);
                break;
            case "Close":
                position.set(Position.KEY_DOOR, false);
                break;
            default:
                break;
        }

        position.set(Position.KEY_ACCELERATION, parser.nextDouble());
        position.set("nfc", parser.next());
        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_RSSI, parser.nextInt());

        return position;
    }

}
