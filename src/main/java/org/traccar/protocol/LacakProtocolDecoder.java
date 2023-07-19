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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class LacakProtocolDecoder extends BaseHttpProtocolDecoder {

    public LacakProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        JsonObject root = Json.createReader(
                new StringReader(request.content().toString(StandardCharsets.US_ASCII))).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, root.getString("device_id"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        JsonObject location = root.getJsonObject("location");

        position.setTime(DateUtil.parseDate(location.getString("timestamp")));

        if (location.containsKey("coords")) {
            JsonObject coordinates = location.getJsonObject("coords");
            position.setLatitude(coordinates.getJsonNumber("latitude").doubleValue());
            position.setLongitude(coordinates.getJsonNumber("longitude").doubleValue());
            position.setAccuracy(coordinates.getJsonNumber("accuracy").doubleValue());
            position.setSpeed(coordinates.getJsonNumber("speed").doubleValue());
            position.setCourse(coordinates.getJsonNumber("heading").doubleValue());
            position.setAltitude(coordinates.getJsonNumber("altitude").doubleValue());
        }

        if (location.containsKey("event")) {
            position.set(Position.KEY_EVENT, location.getString("event"));
        }
        if (location.containsKey("is_moving")) {
            position.set(Position.KEY_MOTION, location.getBoolean("is_moving"));
        }
        if (location.containsKey("odometer")) {
            position.set(Position.KEY_ODOMETER, location.getInt("odometer"));
        }
        if (location.containsKey("mock")) {
            position.set("mock", location.getBoolean("mock"));
        }
        if (location.containsKey("activity")) {
            position.set("activity", location.getJsonObject("activity").getString("type"));
        }
        if (location.containsKey("battery")) {
            JsonObject battery = location.getJsonObject("battery");
            position.set(Position.KEY_BATTERY_LEVEL, (int) (battery.getJsonNumber("level").doubleValue() * 100));
            position.set(Position.KEY_CHARGE, battery.getBoolean("is_charging"));
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

}
