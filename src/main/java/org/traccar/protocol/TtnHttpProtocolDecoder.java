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
import jakarta.json.*;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.model.Network;
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
        if (contentType != null && contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString())) {
            return decodeJson(channel, remoteAddress, request);
        } else {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }
    }

    private void PositionSetFromJson(Position position, String key, JsonValue value) {
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
                // Ignore other types
                break;
        }
    }

    private Object decodeJson(
            Channel channel, SocketAddress remoteAddress, FullHttpRequest request) throws Exception {

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
        // Join messages don't have a payload, but we still need to respond with success to prevent being disabled
        if (payload == null) {
            sendResponse(channel, HttpResponseStatus.NO_CONTENT);
            return null;
        }

        Network network = new Network();
        network.setRadioType("lora");
        network.setCarrier(message.getJsonObject("network_ids").getString("tenant_id"));
        position.setNetwork(network);

        if (message.getJsonArray("rx_metadata").getJsonObject(0).containsKey("time")) {
            // rx_metadata doesn't seem consistent across gateways, I don't know if time is always present
            position.setTime(DateUtil.parseDate(message.getJsonArray("rx_metadata")
                    .getJsonObject(0).getString("time")));
        } else {
            // Fallback to message "received_at" for TTN's Network Server
            position.setTime(DateUtil.parseDate(message.getString("received_at")));
        }

        // Loop over all position keys in the Position class
        for (Field field : Position.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            // Skip anything that is not a public static final String
            if (!(Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)
                    && field.getType().equals(String.class))) {
                continue;
            }
            // Skip non keys
            if (!(field.getName().startsWith("KEY_"))) {
                continue;
            }

            // Get the value of the position key
            String posKey = (String) field.get(null);

            // Loop over all entries in the payload
            for (String jsonKey : payload.keySet()) {
                // Check if the jsonKey matches the posKey or has an underscore after (for CayenneLPP's index)
                if (posKey.equals(jsonKey) || jsonKey.startsWith(posKey + '_')) {
                    PositionSetFromJson(position, posKey, payload.get(jsonKey));
                }
            }
        }

        // Map some CayenneLPP values and other common variations
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

        // Common variations and coordinates
        for (String jsonKey : payload.keySet()) {
            for (String prefix : prefixMappings.keySet()) {
                if (jsonKey.startsWith(prefix + "_")) {
                    int suffix = Integer.parseInt(jsonKey.substring((prefix + "_").length()));
                    // CayenneLPP uses 0-based indexing, "Start with 1 not 0"
                    suffix += 1;
                    String posKey = prefixMappings.get(prefix) + suffix;
                    PositionSetFromJson(position, posKey, payload.get(jsonKey));
                }
            }
            // TTN's CayenneLPP decoder uses a 'gps_*' key for coordinates
            if (jsonKey.startsWith("gps_")) {
                JsonObject coordinates = payload.getJsonObject(jsonKey);
                if (coordinates.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }
                position.setValid(true);
                position.setLatitude(coordinates.getJsonNumber("latitude").doubleValue());
                position.setLongitude(coordinates.getJsonNumber("longitude").doubleValue());
                position.setAltitude(coordinates.getJsonNumber("altitude").doubleValue());
            } else switch (jsonKey) {
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
                    // If the device provides its own timestamp, use that for the fix time
                    switch (payload.get("time").getValueType()) {
                        case STRING:
                            position.setFixTime(DateUtil.parseDate(payload.getString("time")));
                            break;
                        case NUMBER:
                            long timestamp = payload.getJsonNumber("time").longValue();
                            // Check if the timestamp is in seconds or milliseconds
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

        if (!foundSats) {
            // If no satellites were provided, assume valid if we have lat/lng
            position.setValid(foundLat && foundLng);
        }

        if (!(foundLat && foundLng)) {
            // Prevent 0,0 coordinates if lat/lng were not provided
            getLastLocation(position, null);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

    @Override
    protected void sendQueuedCommands(Channel channel, SocketAddress remoteAddress, long deviceId) {
    }

}
