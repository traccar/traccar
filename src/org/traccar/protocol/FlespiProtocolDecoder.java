/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedList;
import java.util.Date;

public class FlespiProtocolDecoder extends BaseProtocolDecoder {

    private final String protocolName;

    public FlespiProtocolDecoder(FlespiProtocol protocol) {
        super(protocol);
        protocolName = protocol.getName();
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        JsonArray result = Json.createReader(new StringReader(request.getContent().toString(StandardCharsets.UTF_8)))
                .readArray();
        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < result.size(); i++) {
            JsonObject message = result.getJsonObject(i);
            String ident = message.getString("ident");
            if (ident == null) {
                continue;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ident);
            if (deviceSession == null) {
                continue;
            }
            Position position = new Position();
            position.setDeviceId(deviceSession.getDeviceId());
            decodePosition(message, position);
            positions.add(position);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return positions;
    }

    private void sendResponse(Channel channel, HttpResponseStatus status) {
        if (channel != null) {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
            response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, 0);
            channel.write(response);
        }
    }

    private void decodePosition(JsonObject object, Position position) {
        position.setProtocol(protocolName);

        position.setTime(new Date((long) object.getJsonNumber("timestamp").doubleValue() * 1000));
        JsonNumber lat = object.getJsonNumber("position.latitude");
        JsonNumber lon = object.getJsonNumber("position.longitude");
        position.setLatitude(lat != null ? lat.doubleValue() : 0);
        position.setLongitude(lon != null ? lon.doubleValue() : 0);

        JsonNumber speed = object.getJsonNumber("position.speed");
        position.setSpeed(speed != null ? speed.doubleValue() : 0);

        JsonNumber course = object.getJsonNumber("position.direction");
        position.setCourse(course != null ? course.doubleValue() : 0);

        JsonNumber altitude = object.getJsonNumber("position.altitude");
        position.setAltitude(altitude != null ? altitude.doubleValue() : 0);

        int satellites = object.getInt("position.satellites", 0);
        position.setValid(object.getBoolean("position.valid", true));
        position.set(Position.KEY_SATELLITES, satellites);

        if (object.getBoolean("alarm.event.trigger", false)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }
    }
}
