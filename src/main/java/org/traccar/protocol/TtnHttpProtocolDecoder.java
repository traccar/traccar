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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

        for (Field field : Position.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!(Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)
                    && field.getType().equals(String.class))) {
                continue;
            }
            if (!(field.getName().startsWith("KEY_"))) {
                continue;
            }

            String posKey = (String) field.get(null);

            for (String jsonKey : payload.keySet()) {
                if (posKey.equals(jsonKey) || jsonKey.startsWith(posKey + '_')) {
                    decodeJsonValue(position, posKey, payload.get(jsonKey));
                }
            }
        }

        Map<String, String> prefixMappings = new HashMap<>() {{
            put("digital_in", Position.PREFIX_IN);
            put("digital_out", Position.PREFIX_OUT);
            put("analog_in", Position.PREFIX_ADC);
            put("presence", Position.PREFIX_COUNT);
            put("temp", Position.PREFIX_TEMP);
            put("temperature", Position.PREFIX_TEMP);
        }};

        boolean foundLat = false;
        boolean foundLng = false;
        boolean foundSats = false;

        for (String jsonKey : payload.keySet()) {
            for (String prefix : prefixMappings.keySet()) {
                if (jsonKey.startsWith(prefix + "_")) {
                    int suffix = Integer.parseInt(jsonKey.substring((prefix + "_").length()));
                    suffix += 1;
                    String posKey = prefixMappings.get(prefix) + suffix;
                    decodeJsonValue(position, posKey, payload.get(jsonKey));
                }
            }
            if (jsonKey.startsWith("gps_")) {
                JsonObject coordinates = payload.getJsonObject(jsonKey);
                if (coordinates.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                position.setValid(true);
                position.setLatitude(coordinates.getJsonNumber("latitude").doubleValue());
                position.setLongitude(coordinates.getJsonNumber("longitude").doubleValue());
                position.setAltitude(coordinates.getJsonNumber("altitude").doubleValue());
            } else {
                switch (jsonKey) {
                    case "latitude":
                    case "lat":
                        position.setLatitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundLat = true;
                        break;
                    case "longitude":
                    case "lng":
                        position.setLongitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundLng = true;
                        break;
                    case "altitude":
                    case "alt":
                        position.setAltitude(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "sat":
                    case "sats":
                    case "satellites":
                        position.setValid(payload.getJsonNumber(jsonKey).doubleValue() > 0);
                        position.set(Position.KEY_SATELLITES, payload.getJsonNumber(jsonKey).intValue());
                        foundSats = true;
                        break;
                    case "speed":
                        position.setSpeed(convertSpeed(payload.getJsonNumber(jsonKey).doubleValue(), "kn"));
                        break;
                    case "bearing":
                    case "heading":
                        position.setCourse(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "accuracy":
                        position.setAccuracy(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "time":
                        switch (payload.get("time").getValueType()) {
                            case STRING:
                                position.setFixTime(DateUtil.parseDate(payload.getString("time")));
                                break;
                            case NUMBER:
                                long timestamp = payload.getJsonNumber("time").longValue();
                                if (timestamp > 10000000000L) {
                                    position.setFixTime(new Date(timestamp));
                                } else {
                                    position.setFixTime(new Date(timestamp * 1000));
                                }
                                break;
                        }
                        break;
                }
            }
        }

        if (!foundSats) {
            position.setValid(foundLat && foundLng);
        }

        if (!(foundLat && foundLng)) {
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
