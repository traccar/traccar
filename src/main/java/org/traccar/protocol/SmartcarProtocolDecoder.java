/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SmartcarProtocolDecoder extends BaseHttpProtocolDecoder {

    public SmartcarProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        JsonObject root = Json.createReader(
                new StringReader(request.content().toString(StandardCharsets.UTF_8))).readObject();

        String eventType = root.getString("eventType");
        JsonObject data = root.getJsonObject("data");

        if ("VERIFY".equals(eventType)) {
            String challenge = data.getString("challenge");
            String managementToken = getConfig().getString(Keys.SMARTCAR_MANAGEMENT_TOKEN);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(managementToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signedChallenge = DataConverter.printHex(mac.doFinal(challenge.getBytes(StandardCharsets.UTF_8)));
            sendResponse(channel, HttpResponseStatus.OK, Unpooled.copiedBuffer(
                    Json.createObjectBuilder().add("challenge", signedChallenge).build().toString(),
                    StandardCharsets.UTF_8));
            return null;
        }

        if (!"VEHICLE_STATE".equals(eventType)) {
            sendResponse(channel, HttpResponseStatus.OK);
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, root.getString("vehicleId"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(new Date(root.getJsonObject("meta").getJsonNumber("deliveredAt").longValue()));

        JsonArray signals = data.getJsonArray("signals");
        for (JsonValue signalValue : signals) {
            JsonObject signal = signalValue.asJsonObject();
            JsonObject body = signal.getJsonObject("body");
            String code = signal.getString("code");
            JsonValue value = body.get("value");
            String unit = body.getString("unit", null);

            switch (code) {
                case "location-preciselocation" -> {
                    position.setValid(true);
                    position.setLatitude(body.getJsonNumber("latitude").doubleValue());
                    position.setLongitude(body.getJsonNumber("longitude").doubleValue());
                    JsonObject meta = signal.getJsonObject("meta");
                    position.setFixTime(new Date(meta.getJsonNumber("oemUpdatedAt").longValue()));
                    position.setDeviceTime(new Date(meta.getJsonNumber("fetchedAt").longValue()));
                }
                case "tractionbattery-stateofcharge" -> {
                    if (value instanceof JsonNumber number) {
                        position.set(Position.KEY_BATTERY_LEVEL, number.doubleValue());
                    }
                }
                case "charge-ischarging" -> {
                    if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
                        position.set(Position.KEY_CHARGE, value == JsonValue.TRUE);
                    }
                }
                case "charge-voltage" -> {
                    if (value instanceof JsonNumber number) {
                        position.set(Position.KEY_POWER, number.doubleValue());
                    }
                }
                case "odometer" -> {
                    if (value instanceof JsonNumber number) {
                        position.set(Position.KEY_ODOMETER,
                                "miles".equalsIgnoreCase(unit) ? number.doubleValue() * 1609.344
                                        : number.doubleValue() * 1000.0);
                    }
                }
                case "speed" -> {
                    if (value instanceof JsonNumber number) {
                        position.setSpeed("miles_per_hour".equalsIgnoreCase(unit)
                                ? UnitsConverter.knotsFromMph(number.doubleValue())
                                : UnitsConverter.knotsFromKph(number.doubleValue()));
                    }
                }
                default -> {
                    if (value instanceof JsonNumber number) {
                        position.set(code, number.doubleValue());
                    } else if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
                        position.set(code, value == JsonValue.TRUE);
                    }
                }
            }
        }

        if (!position.getValid()) {
            getLastLocation(position, position.getDeviceTime());
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

}
