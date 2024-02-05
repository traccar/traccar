/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.util.Date;

public class StbProtocolDecoder extends BaseProtocolDecoder {

    public StbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 110;
    public static final int MSG_PROPERTY = 310;
    public static final int MSG_ALARM = 410;

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, int type, String deviceId, JsonObject root) {
        String response = String.format(
                "{ \"msgType\": %d, \"devId\": \"%s\", \"result\": 1, \"txnNo\": \"%s\" }",
                type + 1, deviceId, root.getString("txnNo"));
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        JsonObject root = Json.createReader(new StringReader((String) msg)).readObject();
        int type = root.getInt("msgType");
        String deviceId = root.getString("devId");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, remoteAddress, type, deviceId, root);

        if (type == MSG_PROPERTY || type == MSG_ALARM) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (type == MSG_PROPERTY) {
                int locationType = 0;
                for (JsonValue property : root.getJsonArray("attrList")) {
                    JsonObject propertyObject = property.asJsonObject();
                    String id = propertyObject.getString("id");
                    switch (id) {
                        case "01101001":
                            locationType = Integer.parseInt(propertyObject.getString("value"));
                            break;
                        case "01102001":
                            position.setLongitude(
                                    Double.parseDouble(propertyObject.getString("value")));
                            break;
                        case "01103001":
                            position.setLatitude(
                                    Double.parseDouble(propertyObject.getString("value")));
                            break;
                        case "01118001":
                            position.set(
                                    Position.KEY_DEVICE_TEMP, Double.parseDouble(propertyObject.getString("value")));
                            break;
                        case "01122001":
                            position.set(
                                    "batteryControl", Integer.parseInt(propertyObject.getString("value")));
                            break;
                        case "02301001":
                            position.set(
                                    "switchCabinetCommand", Integer.parseInt(propertyObject.getString("value")));
                            break;
                        default:
                            String key = "id" + id;
                            if (propertyObject.containsKey("doorId")) {
                                key += "Door" + propertyObject.getString("doorId");
                            }
                            position.set(key, propertyObject.getString("value"));
                            break;
                    }
                }
                if (locationType > 0) {
                    position.setTime(new Date());
                    position.setValid(locationType != 5);
                    if (locationType == 2 || locationType == 4) {
                        position.setLongitude(-position.getLongitude());
                    }
                    if (locationType == 3 || locationType == 4) {
                        position.setLatitude(-position.getLatitude());
                    }
                } else {
                    getLastLocation(position, null);
                }
            }

            return position;
        }

        return null;
    }

}
