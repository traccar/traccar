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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class Pt60ProtocolDecoder extends BaseProtocolDecoder {

    public Pt60ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_G_TRACK = 6;
    public static final int MSG_G_STEP_COUNT = 13;
    public static final int MSG_G_HEART_RATE = 14;

    public static final int MSG_B_POSITION = 1;

    private static final Pattern PATTERN = new PatternBuilder()
            .expression("@(.)#@[,|]")            // header
            .number("V?dd[,|]")                  // protocol version
            .number("(d+)[,|]")                  // type
            .number("(d+)[,|]")                  // imei
            .number("d+[,|]")                    // imsi
            .groupBegin()
            .expression("[^,|]+[,|]").optional() // firmware version
            .number("[01][,|]")                  // state
            .number("d+[,|]")                    // battery
            .groupEnd("?")
            .number("(dddd)(dd)(dd)")            // date (yyyymmdd)
            .number("(dd)(dd)(dd)[,|]")          // time (hhmmss)
            .expression("(.*)")                  // data
            .expression("[,|]")
            .compile();

    private void sendResponse(Channel channel, SocketAddress remoteAddress, String format, int type, String imei) {
        if (channel != null) {
            String message;
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            if (format.equals("G")) {
                message = String.format("@G#@,V01,38,%s,@R#@", time);
            } else {
                message = String.format("@B#@|01|%03d|%s|0|%s|@E#@", type + 1, imei, time);
            }
            channel.writeAndFlush(new NetworkMessage(message, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        String format = parser.next();
        int type = parser.nextInt();
        String imei = parser.next();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, remoteAddress, format, type, imei);

        if (format.equals("G")) {

            if (type != MSG_G_TRACK && type != MSG_G_STEP_COUNT && type != MSG_G_HEART_RATE) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setDeviceTime(parser.nextDateTime());

            String[] values = parser.next().split(",");

            if (type == MSG_G_TRACK) {

                position.setValid(true);
                position.setFixTime(position.getDeviceTime());

                String[] coordinates = values[0].split(";");
                position.setLatitude(Double.parseDouble(coordinates[0]));
                position.setLongitude(Double.parseDouble(coordinates[1]));

            } else {

                getLastLocation(position, position.getDeviceTime());

                switch (type) {
                    case MSG_G_STEP_COUNT:
                        position.set(Position.KEY_STEPS, Integer.parseInt(values[0]));
                        break;
                    case MSG_G_HEART_RATE:
                        position.set(Position.KEY_HEART_RATE, Integer.parseInt(values[0]));
                        position.set(Position.KEY_BATTERY, Integer.parseInt(values[1]));
                        break;
                    default:
                        break;
                }

            }

            return position;

        } else {

            if (type != MSG_B_POSITION) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setDeviceTime(parser.nextDateTime());

            String[] values = parser.next().split("\\|");

            if (Integer.parseInt(values[values.length - 1]) == 2) {

                getLastLocation(position, position.getDeviceTime());

                Network network = new Network();

                for (int i = 0; i < values.length - 1; i++) {
                    String[] cellValues = values[i].split(",");
                    CellTower tower = new CellTower();
                    tower.setCellId(Long.parseLong(cellValues[0]));
                    tower.setLocationAreaCode(Integer.parseInt(cellValues[1]));
                    tower.setMobileNetworkCode(Integer.parseInt(cellValues[2]));
                    tower.setMobileCountryCode(Integer.parseInt(cellValues[3]));
                    tower.setSignalStrength(Integer.parseInt(cellValues[4]));
                    network.addCellTower(tower);
                }

                position.setNetwork(network);


            } else {

                position.setValid(true);
                position.setFixTime(position.getDeviceTime());

                position.setLatitude(Double.parseDouble(values[0]));
                position.setLongitude(Double.parseDouble(values[1]));

            }

            return position;

        }
    }

}
