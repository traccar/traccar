/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class TrvProtocolDecoder extends BaseProtocolDecoder {

    public TrvProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("[A-Z]{2,3}")
            .expression("[A-Z]P")
            .number("dd")
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .expression("([AV])")                // validity
            .number("(dd)(dd.d+)")               // latitude
            .expression("([NS])")
            .number("(ddd)(dd.d+)")              // longitude
            .expression("([EW])")
            .number("(ddd.d)")                   // speed
            .number("(dd)(dd)(dd)")              // time (hhmmss)
            .number("([d.]{6})")                 // course
            .number("(ddd)")                     // gsm
            .number("(ddd)")                     // satellites
            .number("(ddd)")                     // battery
            .number("(d)")                       // acc
            .number("(dd)")                      // arm status
            .number("(dd)")                      // working mode
            .number("(?:d{3,5})?,")
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .number("(d+),")                     // lac
            .number("(d+)")                      // cell
            .groupBegin()
            .text(",")
            .expression("(")
            .groupBegin()
            .expression("[^\\|]+")               // name
            .number("|xx-xx-xx-xx-xx-xx")        // mac
            .number("|d+&?")                     // signal
            .groupEnd("+")
            .expression(")")
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_HEARTBEAT = new PatternBuilder()
            .expression("[A-Z]{2,3}")
            .text("CP01,")
            .number("(ddd)")                     // gsm
            .number("(ddd)")                     // gps
            .number("(ddd)")                     // battery
            .number("(d)")                       // acc
            .number("(dd)")                      // arm status
            .number("(dd)")                      // working mode
            .groupBegin()
            .number("(ddd)")                     // interval
            .number("d")                         // vibration alarm
            .number("ddd")                       // vibration sensitivity
            .number("d")                         // automatic arm
            .number("dddd")                      // automatic arm time
            .number("(d)")                       // blocked
            .number("(d)")                       // power status
            .number("(d)")                       // movement status
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_LBS = new PatternBuilder()
            .expression("[A-Z]{2,3}")
            .text("AP02,")
            .expression("[^,]+,")                // language
            .number("[01],")                     // reply
            .number("d+,")                       // cell count
            .number("(d+),")                     // mcc
            .number("(d+),")                     // mnc
            .expression("(")
            .groupBegin()
            .number("d+|")                       // lac
            .number("d+|")                       // cid
            .number("d+,")                       // rssi
            .groupEnd("+")
            .expression(")")
            .number("d+,")                       // wifi count
            .expression("(.*)")                  // wifi
            .compile();

    private Boolean decodeOptionalValue(Parser parser, int activeValue) {
        int value = parser.nextInt();
        if (value != 0) {
            return value == activeValue;
        }
        return null;
    }

    private void decodeCommon(Position position, Parser parser) {

        position.set(Position.KEY_RSSI, parser.nextInt());
        position.set(Position.KEY_SATELLITES, parser.nextInt());
        position.set(Position.KEY_BATTERY, parser.nextInt());
        position.set(Position.KEY_IGNITION, decodeOptionalValue(parser, 1));
        position.set(Position.KEY_ARMED, decodeOptionalValue(parser, 1));

        int mode = parser.nextInt();
        if (mode != 0) {
            position.set("mode", mode);
        }
    }

    private void decodeWifi(Network network, String data) {
        for (String wifi : data.split("&")) {
            if (!wifi.isEmpty()) {
                String[] values = wifi.split("\\|");
                network.addWifiAccessPoint(WifiAccessPoint.from(
                        values[1].replace('-', ':'), Integer.parseInt(values[2])));
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        String id = sentence.startsWith("TRV") ? sentence.substring(0, 3) : sentence.substring(0, 2);
        String type = sentence.substring(id.length(), id.length() + 4);

        if (channel != null) {
            String responseHeader = id + (char) (type.charAt(0) + 1) + type.substring(1);
            if (type.equals("AP00") && id.equals("IW")) {
                String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                channel.writeAndFlush(new NetworkMessage(responseHeader + "," + time + ",0#", remoteAddress));
            } else if (type.equals("AP14")) {
                channel.writeAndFlush(new NetworkMessage(responseHeader + ",0.000,0.000#", remoteAddress));
            } else {
                channel.writeAndFlush(new NetworkMessage(responseHeader + "#", remoteAddress));
            }
        }

        if (type.equals("AP00")) {
            getDeviceSession(channel, remoteAddress, sentence.substring(id.length() + type.length()));
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (type.equals("CP01")) {

            Parser parser = new Parser(PATTERN_HEARTBEAT, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            decodeCommon(position, parser);

            if (parser.hasNext(3)) {
                position.set(Position.KEY_BLOCKED, decodeOptionalValue(parser, 2));
                position.set(Position.KEY_CHARGE, decodeOptionalValue(parser, 1));
                position.set(Position.KEY_MOTION, decodeOptionalValue(parser, 1));
            }

            return position;

        } else if (type.equals("AP01") || type.equals("AP10") || type.equals("YP03") || type.equals("YP14")) {

            Parser parser = new Parser(PATTERN, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            DateBuilder dateBuilder = new DateBuilder()
                    .setDate(parser.nextInt(), parser.nextInt(), parser.nextInt());

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate());
            position.setLongitude(parser.nextCoordinate());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

            dateBuilder.setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
            position.setTime(dateBuilder.getDate());

            position.setCourse(parser.nextDouble());

            decodeCommon(position, parser);

            Network network = new Network();

            network.addCellTower(CellTower.from(
                    parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt()));

            if (parser.hasNext()) {
                decodeWifi(network, parser.next());
            }

            position.setNetwork(network);

            return position;

        } else if (type.equals("AP02")) {

            Parser parser = new Parser(PATTERN_LBS, sentence);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            int mcc = parser.nextInt();
            int mnc = parser.nextInt();

            Network network = new Network();

            for (String cell : parser.next().split(",")) {
                if (!cell.isEmpty()) {
                    String[] values = cell.split("\\|");
                    network.addCellTower(CellTower.from(
                            mcc, mnc,
                            Integer.parseInt(values[0]),
                            Integer.parseInt(values[1]),
                            Integer.parseInt(values[2])));
                }
            }

            decodeWifi(network, parser.next());

            position.setNetwork(network);

            return position;

        }

        return null;
    }

}
