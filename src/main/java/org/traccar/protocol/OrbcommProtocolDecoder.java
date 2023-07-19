/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.http.FullHttpResponse;
import org.traccar.BasePipelineFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

public class OrbcommProtocolDecoder extends BaseProtocolDecoder {

    public OrbcommProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpResponse response = (FullHttpResponse) msg;
        String content = response.content().toString(StandardCharsets.UTF_8);
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        if (channel != null && !json.getString("NextStartUTC").isEmpty()) {
            OrbcommProtocolPoller poller =
                    BasePipelineFactory.getHandler(channel.pipeline(), OrbcommProtocolPoller.class);
            if (poller != null) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                poller.setStartTime(dateFormat.parse(json.getString("NextStartUTC")));
            }
        }

        if (json.get("Messages").getValueType() == JsonValue.ValueType.NULL) {
            return null;
        }

        LinkedList<Position> positions = new LinkedList<>();

        JsonArray messages = json.getJsonArray("Messages");
        for (int i = 0; i < messages.size(); i++) {
            JsonObject message = messages.getJsonObject(i);
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, message.getString("MobileID"));
            if (deviceSession != null) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                position.setDeviceTime(dateFormat.parse(message.getString("MessageUTC")));

                JsonArray fields = message.getJsonObject("Payload").getJsonArray("Fields");
                for (int j = 0; j < fields.size(); j++) {
                    JsonObject field = fields.getJsonObject(j);
                    String value = field.getString("Value");
                    switch (field.getString("Name").toLowerCase()) {
                        case "eventtime":
                            position.setDeviceTime(new Date(Long.parseLong(value) * 1000));
                            break;
                        case "latitude":
                            position.setLatitude(Integer.parseInt(value) / 60000.0);
                            break;
                        case "longitude":
                            position.setLongitude(Integer.parseInt(value) / 60000.0);
                            break;
                        case "speed":
                            position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(value)));
                            break;
                        case "heading":
                            int heading = Integer.parseInt(value);
                            position.setCourse(heading <= 360 ? heading : 0);
                            break;
                        default:
                            break;
                    }
                }

                if (position.getLatitude() != 0 && position.getLongitude() != 0) {
                    position.setValid(true);
                    position.setFixTime(position.getDeviceTime());
                } else {
                    getLastLocation(position, position.getDeviceTime());
                }

                positions.add(position);

            }
        }

        return positions.isEmpty() ? null : positions;
    }

}
