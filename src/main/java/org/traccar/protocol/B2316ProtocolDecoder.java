/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class B2316ProtocolDecoder extends BaseProtocolDecoder {

    public B2316ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String decodeAlarm(int value) {
        return switch (value) {
            case 1 -> Position.ALARM_LOW_BATTERY;
            case 2 -> Position.ALARM_SOS;
            case 3 -> Position.ALARM_POWER_OFF;
            case 4 -> Position.ALARM_REMOVING;
            default -> null;
        };
    }

    private Integer decodeBattery(int value) {
        return switch (value) {
            case 0 -> 10;
            case 1 -> 30;
            case 2 -> 60;
            case 3 -> 80;
            case 4 -> 100;
            default -> null;
        };
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        JsonObject root = Json.createReader(new StringReader((String) msg)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, root.getString("imei"));
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        JsonArray data = root.getJsonArray("data");
        for (int i = 0; i < data.size(); i++) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Network network = new Network();

            JsonObject item = data.getJsonObject(i);
            Date time = new Date(item.getJsonNumber("tm").longValue() * 1000);

            if (item.containsKey("gp")) {
                String[] coordinates = item.getString("gp").split(",");
                position.setLongitude(Double.parseDouble(coordinates[0]));
                position.setLatitude(Double.parseDouble(coordinates[1]));
                position.setValid(true);
                position.setTime(time);
            } else {
                getLastLocation(position, time);
            }

            if (item.containsKey("ci")) {
                String[] cell = item.getString("ci").split(",");
                network.addCellTower(CellTower.from(
                        Integer.parseInt(cell[0]), Integer.parseInt(cell[1]),
                        Integer.parseInt(cell[2]), Integer.parseInt(cell[3]),
                        Integer.parseInt(cell[4])));
            }

            if (item.containsKey("wi")) {
                String[] points = item.getString("wi").split(";");
                for (String point : points) {
                    String[] values = point.split(",");
                    String mac = values[0].replaceAll("(..)", "$1:");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            mac.substring(0, mac.length() - 1), Integer.parseInt(values[1])));
                }
            }

            if (item.containsKey("wn")) {
                position.addAlarm(decodeAlarm(item.getInt("wn")));
            }
            if (item.containsKey("ic")) {
                position.set(Position.KEY_ICCID, item.getString("ic"));
            }
            if (item.containsKey("ve")) {
                position.set(Position.KEY_VERSION_FW, item.getString("ve"));
            }
            if (item.containsKey("te")) {
                String[] temperatures = item.getString("te").split(",");
                for (int j = 0; j < temperatures.length; j++) {
                    position.set(Position.PREFIX_TEMP + (j + 1), Integer.parseInt(temperatures[j]) * 0.1);
                }
            }
            if (item.containsKey("st")) {
                position.set(Position.KEY_STEPS, item.getInt("st"));
            }
            if (item.containsKey("ba")) {
                position.set(Position.KEY_BATTERY_LEVEL, decodeBattery(item.getInt("ba")));
            }
            if (item.containsKey("sn")) {
                position.set(Position.KEY_RSSI, item.getInt("sn"));
            }
            if (item.containsKey("hr")) {
                position.set(Position.KEY_HEART_RATE, item.getInt("hr"));
            }

            if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
                position.setNetwork(network);
            }

            positions.add(position);
        }

        return positions.isEmpty() ? null : positions;
    }

}
