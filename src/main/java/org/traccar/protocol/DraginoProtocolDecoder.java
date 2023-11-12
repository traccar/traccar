/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateUtil;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class DraginoProtocolDecoder extends BaseHttpProtocolDecoder {

    public DraginoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String content = request.content().toString(StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        String deviceId = json.getJsonObject("end_device_ids").getString("device_id");
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        JsonObject message = json.getJsonObject("uplink_message");
        JsonObject decoded = message.getJsonObject("decoded_payload");

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(DateUtil.parseDate(message.getString("received_at")));

        position.setValid(true);
        position.setLatitude(decoded.getJsonNumber("Latitude").doubleValue());
        position.setLongitude(decoded.getJsonNumber("Longitude").doubleValue());

        position.set("humidity", decoded.getJsonNumber("Hum").doubleValue());
        position.set(Position.KEY_BATTERY, decoded.getJsonNumber("BatV").doubleValue());
        position.set(Position.PREFIX_TEMP + 1, decoded.getJsonNumber("Tem").doubleValue());

        if (Boolean.parseBoolean(decoded.getString("ALARM_status"))) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

}
