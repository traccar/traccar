/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class PolteProtocolDecoder extends BaseHttpProtocolDecoder {

    public PolteProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String content = request.content().toString(StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, json.getString("ueToken"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        if (json.containsKey("location")) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            JsonObject location = json.getJsonObject("location");

            position.setValid(true);
            position.setTime(new Date(location.getInt("detected_at") * 1000L));
            position.setLatitude(location.getJsonNumber("latitude").doubleValue());
            position.setLongitude(location.getJsonNumber("longitude").doubleValue());
            position.setAltitude(location.getJsonNumber("altitude").doubleValue());

            if (json.containsKey("report")) {
                JsonObject report = json.getJsonObject("report");
                position.set(Position.KEY_EVENT, report.getInt("event"));
                if (report.containsKey("battery")) {
                    JsonObject battery = report.getJsonObject("battery");
                    position.set(Position.KEY_BATTERY_LEVEL, battery.getInt("level"));
                    position.set(Position.KEY_BATTERY, battery.getJsonNumber("voltage").doubleValue());
                }
            }

            return position;

        }

        sendResponse(channel, HttpResponseStatus.OK);
        return null;
    }

}
