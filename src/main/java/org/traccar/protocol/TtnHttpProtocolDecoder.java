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
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
            } else if (jsonKey.startsWith("digital_in_")) {
                int suffix = Integer.parseInt(jsonKey.substring(("digital_in_").length()));
                suffix += 1;
                position.set(Position.PREFIX_IN + suffix, payload.getJsonNumber(jsonKey).doubleValue());
            } else if (jsonKey.startsWith("digital_out_")) {
                int suffix = Integer.parseInt(jsonKey.substring(("digital_out_").length()));
                suffix += 1;
                position.set(Position.PREFIX_OUT + suffix, payload.getJsonNumber(jsonKey).doubleValue());
            } else if (jsonKey.startsWith("analog_in_")) {
                int suffix = Integer.parseInt(jsonKey.substring(("analog_in_").length()));
                suffix += 1;
                position.set(Position.PREFIX_ADC + suffix, payload.getJsonNumber(jsonKey).doubleValue());
            } else if (jsonKey.startsWith("temperature_")) {
                int suffix = Integer.parseInt(jsonKey.substring(("temperature_").length()));
                suffix += 1;
                position.set(Position.PREFIX_TEMP + suffix, payload.getJsonNumber(jsonKey).doubleValue());
            } else if (jsonKey.startsWith("humidity_")) {
                position.set(Position.KEY_HUMIDITY, payload.getJsonNumber(jsonKey).doubleValue());
            } else {
                switch (jsonKey) {
                    case "latitude":
                    case "lat":
                        position.setLatitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundGps = true;
                        break;
                    case "longitude":
                    case "lng":
                        position.setLongitude(payload.getJsonNumber(jsonKey).doubleValue());
                        foundGps = true;
                        break;
                    case "altitude":
                    case "alt":
                        position.setAltitude(payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "sat":
                    case "sats":
                    case "satellites":
                        position.set(Position.KEY_SATELLITES, payload.getJsonNumber(jsonKey).intValue());
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
                    case "hdop":
                        position.set(Position.KEY_HDOP, payload.getJsonNumber(jsonKey).doubleValue());
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
                    case "humidity":
                        position.set(Position.KEY_HUMIDITY, payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "battery":
                        position.set(Position.KEY_BATTERY, payload.getJsonNumber(jsonKey).doubleValue());
                        break;
                    case "alarm":
                        position.set(Position.KEY_ALARM, payload.getString(jsonKey));
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
}
