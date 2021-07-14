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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;

public class StbProtocolDecoder extends BaseProtocolDecoder {

    public StbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 110;
    public static final int MSG_PROPERTY = 310;
    public static final int MSG_ALARM = 410;

    public static class Response {
        @JsonProperty("msgType")
        private int type;
        @JsonProperty("devId")
        private String deviceId;
        @JsonProperty("result")
        private int result;
        @JsonProperty("txnNo")
        private String transaction;
    }

    private void sendResponse(
            Channel channel, SocketAddress remoteAddress, int type, String deviceId, JsonObject root)
            throws JsonProcessingException {

        Response response = new Response();
        response.type = type + 1;
        response.deviceId = deviceId;
        response.result = 1;
        response.transaction = root.getString("txnNo");
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Context.getObjectMapper().writeValueAsString(response), remoteAddress));
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

            getLastLocation(position, null);

            if (type == MSG_PROPERTY) {
                for (JsonValue property : root.getJsonArray("attrList")) {
                    JsonObject propertyObject = property.asJsonObject();
                    String key = "id" + propertyObject.getString("id");
                    if (propertyObject.containsKey("doorId")) {
                        key += "Door" + propertyObject.getString("doorId");
                    }
                    position.set(key, propertyObject.getString("value"));
                }
            }

            return position;
        }

        return null;
    }

}
