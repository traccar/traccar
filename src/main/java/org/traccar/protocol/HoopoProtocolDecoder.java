/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.time.OffsetDateTime;
import java.util.Date;

public class HoopoProtocolDecoder extends BaseProtocolDecoder {

    public HoopoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        JsonObject json = Json.createReader(new StringReader((String) msg)).readObject();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, json.getString("deviceId"));
        if (deviceSession == null) {
            return null;
        }

        if (json.containsKey("eventData")) {

            JsonObject eventData = json.getJsonObject("eventData");

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            Date time = new Date(OffsetDateTime.parse(json.getString("eventTime")).toInstant().toEpochMilli());
            position.setTime(time);

            position.setValid(true);
            position.setLatitude(eventData.getJsonNumber("latitude").doubleValue());
            position.setLongitude(eventData.getJsonNumber("longitude").doubleValue());

            position.set(Position.KEY_EVENT, eventData.getString("eventType"));
            position.set(Position.KEY_BATTERY_LEVEL, eventData.getInt("batteryLevel"));

            if (json.containsKey("movement")) {
                position.setSpeed(json.getJsonObject("movement").getInt("Speed"));
            }

            return position;

        }

        return null;
    }

}
