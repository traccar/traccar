/*
 * Copyright 2025 Stephen Horvath (me@stevetech.au)
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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class TtnHttpProtocolDecoder extends BaseHttpProtocolDecoder {

    public TtnHttpProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString())) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        String content = request.content().toString(StandardCharsets.UTF_8);
        JsonObject root = Json.createReader(new StringReader(content)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress,
                root.getJsonObject("end_device_ids").getString("device_id"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.NOT_FOUND);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        JsonObject message = root.getJsonObject("uplink_message");
        if (message == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        JsonObject payload = message.getJsonObject("decoded_payload");
        if (payload == null) {
            sendResponse(channel, HttpResponseStatus.OK);
            return null;
        }

        if (message.getJsonArray("rx_metadata").getJsonObject(0).containsKey("time")) {
            position.setTime(DateUtil.parseDate(message.getJsonArray("rx_metadata")
                    .getJsonObject(0).getString("time")));
        } else {
            position.setTime(DateUtil.parseDate(message.getString("received_at")));
        }

        boolean foundGps = false;

        for (String jsonKey : payload.keySet()) {
            if (jsonKey.startsWith("gps_")) {
                JsonObject coordinates = payload.getJsonObject(jsonKey);
                if (coordinates.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                position.setLatitude(coordinates.getJsonNumber("latitude").doubleValue());
                position.setLongitude(coordinates.getJsonNumber("longitude").doubleValue());
                position.setAltitude(coordinates.getJsonNumber("altitude").doubleValue());
                foundGps = true;
            } else {
                switch (jsonKey) {
                    case "latitude":
                        position.setLatitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundGps = true;
                        break;
                    case "longitude":
                        position.setLongitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundGps = true;
                        break;
                    case "altitude":
                        position.setAltitude(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "accuracy":
                        position.setAccuracy(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "sats":
                        position.set(Position.KEY_SATELLITES, payload.getJsonNumber(jsonKey).intValue());
                        break;
                    case "speed":
                        position.setSpeed(convertSpeed(payload.getJsonNumber(jsonKey).doubleValue(), "kn"));
                        break;
                    case "heading":
                        position.setCourse(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    default:
                        decodeJsonValue(position, jsonKey, payload.get(jsonKey));
                        break;
                }
            }
        }

        if (foundGps) {
            position.setValid(true);
        } else {
            getLastLocation(position, null);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

    private void decodeJsonValue(Position position, String key, JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                position.set(key, ((JsonString) value).getString());
                break;
            case NUMBER:
                position.set(key, ((JsonNumber) value).doubleValue());
                break;
            case TRUE:
                position.set(key, true);
                break;
            case FALSE:
                position.set(key, false);
                break;
            default:
                break;
        }
    }
}
