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
import javax.json.JsonString;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedList;
import java.util.Date;

public class FlespiProtocolDecoder extends BaseProtocolDecoder {

    public FlespiProtocolDecoder(FlespiProtocol protocol) {
        super(protocol);
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
            JsonString ident = message.getJsonString("ident");
            if (ident == null) {
                continue;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ident.getString());
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
        position.setProtocol(getProtocolName());

        Date deviceTime = new Date((long) object.getJsonNumber("timestamp").doubleValue() * 1000);
        position.setTime(deviceTime);
        JsonNumber lat = object.getJsonNumber("position.latitude");
        JsonNumber lon = object.getJsonNumber("position.longitude");
        if (lat != null && lon != null) {
            position.setLatitude(lat.doubleValue());
            position.setLongitude(lon.doubleValue());
        } else {
            getLastLocation(position, deviceTime);
        }
        JsonNumber speed = object.getJsonNumber("position.speed");
        if (speed != null) {
            position.setSpeed(speed.doubleValue());
        }
        JsonNumber course = object.getJsonNumber("position.direction");
        if (course != null) {
            position.setCourse(course.doubleValue());
        }
        JsonNumber altitude = object.getJsonNumber("position.altitude");
        if (altitude != null) {
            position.setAltitude(altitude.doubleValue());
        }

        position.setValid(object.getBoolean("position.valid", true));
        position.set(Position.KEY_SATELLITES, object.getInt("position.satellites", 0));

        if (object.getBoolean("alarm.event.trigger", false)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }
    }
}
