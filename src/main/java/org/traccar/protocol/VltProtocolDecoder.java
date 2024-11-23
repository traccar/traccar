/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class VltProtocolDecoder extends BaseHttpProtocolDecoder {

    public VltProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(dd)")                      // alert id
            .expression("([HL])")                // history
            .number("([01])")                    // validity
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("(d{3}.d{6})([NS])")         // latitude
            .number("(d{3}.d{6})([EW])")         // longitude
            .number("(d{3})")                    // mcc
            .expression("(x*[0-9]+)")            // mnc
            .number("(x{4})")                    // lac
            .number("(d{9})")                    // cid
            .number("(d{3}.d{2})")               // speed
            .number("(d{3}.d{2})")               // course
            .number("(d{2})")                    // satellites
            .number("(d{2})")                    // hdop
            .number("(d{2})")                    // rssi
            .number("([01])")                    // ignition
            .number("([01])")                    // charging
            .expression("([HMS])")               // vehicle mode
            .compile();

    private Position decodePosition(DeviceSession deviceSession, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_EVENT, parser.nextInt());
        position.set(Position.KEY_ARCHIVE, parser.next().equals("H") ? true : null);

        position.setValid(parser.nextInt() > 0);
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));

        int mcc = parser.nextInt();
        int mnc = Integer.parseInt(parser.next().replaceAll("x", ""));
        int lac = parser.nextHexInt();
        int cid = parser.nextInt();

        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_HDOP, parser.nextInt());

        position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid, parser.nextInt())));

        position.set(Position.KEY_IGNITION, parser.nextInt() > 0);
        position.set(Position.KEY_CHARGE, parser.nextInt() > 0);
        position.set(Position.KEY_MOTION, parser.next().equals("M"));

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(
                request.content().toString(StandardCharsets.US_ASCII), false);
        String sentence = decoder.parameters().get("vltdata").iterator().next();

        int index = 0;
        String type = sentence.substring(index, index += 3);
        String imei = sentence.substring(index, index += 15);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        sendResponse(channel, HttpResponseStatus.OK);

        return switch (type) {
            case "NRM" -> decodePosition(deviceSession, sentence.substring(3 + 15));
            case "BTH" -> {
                List<Position> positions = new LinkedList<>();
                int count = Integer.parseInt(sentence.substring(index, index += 3));
                for (int i = 0; i < count; i++) {
                    positions.add(decodePosition(deviceSession, sentence.substring(index, index += 78)));
                }
                yield positions;
            }
            default -> null;
        };
    }

}
