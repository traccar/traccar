/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class FutureWayProtocolDecoder extends BaseProtocolDecoder {

    public FutureWayProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_GPS = new PatternBuilder()
            .text("GPS:")
            .expression("([AV]),")               // validity
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .groupBegin()
            .number("(dd)(dd.d+)([NS]),")        // latitude
            .number("(ddd)(dd.d+)([EW]),")       // longitude
            .or()
            .number("(d+.d+)([NS]),")            // latitude
            .number("(d+.d+)([EW]),")            // longitude
            .groupEnd()
            .number("(d+.d+),")                  // speed
            .number("(d+.d+)")                   // course
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        ByteBuf header = Unpooled.wrappedBuffer(DataConverter.parseHex(sentence.substring(0, 16)));
        sentence = sentence.substring(16, sentence.length() - 4);

        header.readUnsignedByte(); // header
        header.readUnsignedInt(); // length
        int type = header.readUnsignedByte();
        header.readUnsignedShort(); // index

        if (type == 0x20) {

            getDeviceSession(channel, remoteAddress, sentence.split(",")[1].substring(5));

        } else if (type == 0xA0) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Network network = new Network();

            for (String line : sentence.split("\r\n")) {

                if (line.startsWith("GPS")) {

                    Parser parser = new Parser(PATTERN_GPS, line);
                    if (!parser.matches()) {
                        return null;
                    }

                    position.setValid(parser.next().equals("A"));
                    position.setTime(parser.nextDateTime());

                    if (parser.hasNext(6)) {
                        position.setLatitude(parser.nextCoordinate());
                        position.setLongitude(parser.nextCoordinate());
                    }

                    if (parser.hasNext(4)) {
                        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
                        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
                    }

                    position.setSpeed(parser.nextDouble());
                    position.setCourse(parser.nextDouble());

                } else if (line.startsWith("WIFI")) {

                    if (line.contains(",")) {
                        for (String item : line.substring(line.indexOf(',') + 1).split("&")) {
                            String[] values = item.split("\\|");
                            network.addWifiAccessPoint(
                                    WifiAccessPoint.from(values[1].replace('-', ':'), Integer.parseInt(values[2])));
                        }
                    }

                } else if (line.startsWith("LBS")) {

                    String[] values = line.substring("LBS:".length()).split(",");
                    int lac, cid;
                    if (Integer.parseInt(values[2]) > 65535) {
                        cid = Integer.parseInt(values[2]);
                        lac = Integer.parseInt(values[3]);
                    } else {
                        lac = Integer.parseInt(values[2]);
                        cid = Integer.parseInt(values[3]);
                    }
                    network.addCellTower(CellTower.from(
                            Integer.parseInt(values[0]),
                            Integer.parseInt(values[1]), lac, cid));

                }

            }

            if (!network.getCellTowers().isEmpty() || !network.getWifiAccessPoints().isEmpty()) {
                position.setNetwork(network);
            }

            if (position.getFixTime() == null) {
                getLastLocation(position, null);
            }

            return position;

        }

        return null;
    }

}
