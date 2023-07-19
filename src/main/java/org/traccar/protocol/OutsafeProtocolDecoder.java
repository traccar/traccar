/*
 * Copyright 2019 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class OutsafeProtocolDecoder extends BaseHttpProtocolDecoder {

    public OutsafeProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String content = request.content().toString(StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, json.getString("device"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date());
        position.setValid(true);
        position.setLatitude(json.getJsonNumber("latitude").doubleValue());
        position.setLongitude(json.getJsonNumber("longitude").doubleValue());
        position.setAltitude(json.getJsonNumber("altitude").doubleValue());
        position.setCourse(json.getJsonNumber("heading").intValue());

        position.set(Position.KEY_RSSI, json.getJsonNumber("rssi").intValue());
        position.set("origin", json.getString("origin"));

        JsonObject data = json.getJsonObject("data");
        for (Map.Entry<String, JsonValue> entry : data.entrySet()) {
            decodeUnknownParam(entry.getKey(), entry.getValue(), position);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

    private void decodeUnknownParam(String name, JsonValue value, Position position) {
        if (value instanceof JsonNumber) {
            position.set(name, ((JsonNumber) value).doubleValue());
        } else if (value instanceof JsonString) {
            position.set(name, ((JsonString) value).getString());
        } else if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
            position.set(name, value == JsonValue.TRUE);
        }
    }

}
