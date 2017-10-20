/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.util.CharsetUtil;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.Log;
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
        Log.debug(String.format("messages received msgs_count=%d", result.size()));
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
            response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, 10);
            response.setContent(ChannelBuffers.copiedBuffer("Hello namo", CharsetUtil.US_ASCII));
            channel.write(response);
        }
    }

    private void decodePosition(JsonObject msg, Position position) {
        position.setProtocol("flespi");

        position.setTime(new Date((long) msg.getJsonNumber("timestamp").doubleValue() * 1000));
        JsonNumber lat = msg.getJsonNumber("position.latitude");
        JsonNumber lon = msg.getJsonNumber("position.longitude");
        position.setLatitude((lat != null && lon != null) ? lat.doubleValue() : 0);
        position.setLongitude((lat != null && lon != null) ? lon.doubleValue() : 0);

        JsonNumber speed = msg.getJsonNumber("position.speed");
        position.setSpeed(speed != null ? speed.doubleValue() : 0);
        if (position.getSpeed() == 111) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }

        JsonNumber course = msg.getJsonNumber("position.direction");
        position.setCourse(course != null ? course.doubleValue() : 0);

        JsonNumber altitude = msg.getJsonNumber("position.altitude");
        position.setAltitude(altitude != null ? altitude.doubleValue() : 0);

        int satellites = msg.getInt("position.satellites", 0);
        position.setValid(position.getLatitude() != 0 && position.getLongitude() != 0 && satellites >= 3);
        position.set(Position.KEY_SATELLITES, satellites);

        if (msg.getBoolean("alarm.event.trigger", false)) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }
    }
}
