/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.CellTower;
import org.traccar.model.Command;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RfTrackProtocolDecoder extends BaseHttpProtocolDecoder {

    public RfTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(
                request.content().toString(StandardCharsets.US_ASCII), false);
        Map<String, List<String>> params = decoder.parameters();

        Position position = new Position(getProtocolName());
        Network network = new Network();

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                switch (entry.getKey()) {
                    case "i":
                        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, value);
                        if (deviceSession == null) {
                            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
                            return null;
                        }
                        position.setDeviceId(deviceSession.getDeviceId());
                        break;
                    case "v":
                        position.set(Position.KEY_VERSION_FW, value);
                        break;
                    case "t":
                        position.setDeviceTime(new Date(Long.parseLong(value)));
                        break;
                    case "bat":
                        int battery = Integer.parseInt(value);
                        position.set(Position.KEY_BATTERY_LEVEL, battery & 0xff);
                        position.set("plugStatus", (battery >> 8) & 0x0f);
                        position.set(Position.KEY_CHARGE, ((battery >> 12) & 0x0f) == 1);
                        break;
                    case "id":
                        position.set("braceletId", value);
                        break;
                    case "rc":
                        int braceletCode = Integer.parseInt(value);
                        position.set("braceletCode", braceletCode & 0xffff);
                        position.set("braceletStatus", braceletCode >> 16);
                        break;
                    case "idt":
                        long braceletTime = Long.parseLong(value);
                        position.set("lastHeartbeat", (braceletTime >> 45) * 10);
                        position.set("lastPaired", ((braceletTime >> 30) & 0xffff) * 10);
                        position.set("lastUnpaired", ((braceletTime >> 15) & 0xffff) * 10);
                        break;
                    case "mt":
                        int vibrationTime = Integer.parseInt(value);
                        position.set("vibrationDevice", (vibrationTime & 0x7fff) * 10);
                        position.set("vibrationBracelet", (vibrationTime >> 15) * 10);
                        break;
                    case "gps":
                        JsonObject location = Json.createReader(new StringReader(value)).readObject();
                        position.setValid(true);
                        position.setAccuracy(location.getJsonNumber("a").doubleValue());
                        position.setLongitude(location.getJsonNumber("x").doubleValue());
                        position.setLatitude(location.getJsonNumber("y").doubleValue());
                        position.setAltitude(location.getJsonNumber("z").doubleValue());
                        position.setFixTime(new Date(location.getJsonNumber("t").longValue()));
                        break;
                    case "gsm":
                        JsonObject cellInfo = Json.createReader(new StringReader(value)).readObject();
                        int mcc = cellInfo.getInt("c");
                        int mnc = cellInfo.getInt("n");
                        JsonArray cells = cellInfo.getJsonArray("b");
                        for (int i = 0; i < cells.size(); i++) {
                            JsonObject cell = cells.getJsonObject(i);
                            network.addCellTower(CellTower.from(
                                    mcc, mnc, cell.getInt("l"), cell.getInt("c"), cell.getInt("b")));
                        }
                        break;
                    case "dbm":
                        position.set(Position.KEY_RSSI, Integer.parseInt(value));
                        break;
                    case "bar":
                        position.set("pressure", Double.parseDouble(value));
                        break;
                    case "cob":
                        position.set("pressureChanges", value);
                        break;
                    case "wifi":
                        JsonArray wifiInfo = Json.createReader(new StringReader(value)).readArray();
                        for (int i = 0; i < wifiInfo.size(); i++) {
                            JsonObject wifi = wifiInfo.getJsonObject(i);
                            network.addWifiAccessPoint(WifiAccessPoint.from(
                                    wifi.getString("m").replace('-', ':'), wifi.getInt("l")));
                        }
                        break;
                    case "u_ids":
                        position.set("unpairedIds", value);
                        break;
                    default:
                        break;
                }
            }
        }

        if (position.getFixTime() == null) {
            getLastLocation(position, position.getDeviceTime());
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }

        String response = "{}";
        for (Command command : getCommandsManager().readQueuedCommands(position.getDeviceId(), 1)) {
            response = command.getString(Command.KEY_DATA);
        }
        sendResponse(channel, HttpResponseStatus.OK, Unpooled.copiedBuffer(response, StandardCharsets.UTF_8));
        return position;
    }

}
