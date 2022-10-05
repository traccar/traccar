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
import org.traccar.session.DeviceSession;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
                        position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(value) & 0xff);
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
