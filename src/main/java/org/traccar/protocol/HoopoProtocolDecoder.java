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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;

public class HoopoProtocolDecoder extends BaseHttpProtocolDecoder {

    public HoopoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String content = request.content().toString(StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, json.getString("deviceId"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        if (json.containsKey("eventData")) {

            JsonObject eventData = json.getJsonObject("eventData");

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Date time = new Date(OffsetDateTime.parse(eventData.getString("receiveTime")).toInstant().toEpochMilli());
            position.setTime(time);

            position.setValid(true);
            position.setLatitude(eventData.getJsonNumber("latitude").doubleValue());
            position.setLongitude(eventData.getJsonNumber("longitude").doubleValue());

            position.set(Position.KEY_EVENT, eventData.getString("eventType"));
            position.set(Position.KEY_BATTERY_LEVEL, eventData.getInt("batteryLevel"));

            return position;

        }

        sendResponse(channel, HttpResponseStatus.OK);

        return null;
    }

}
