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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.session.DeviceSession;
import org.traccar.model.Position;

import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JimiPhotoProtocolDecoder extends BaseHttpProtocolDecoder {

    public JimiPhotoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        try {
            Attribute callbackBody = (Attribute) decoder.getBodyHttpData("callbackBody");
            FileUpload file = (FileUpload) decoder.getBodyHttpData("file");

            if (callbackBody == null || file == null) {
                sendJsonResponse(channel, HttpResponseStatus.BAD_REQUEST, 400, "missing field", null);
                return null;
            }

            JsonObject body = Json.createReader(new StringReader(callbackBody.getValue())).readObject();
            String imei = body.getString("imei");

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession == null) {
                sendJsonResponse(channel, HttpResponseStatus.NOT_FOUND, 404, "unknown device", null);
                return null;
            }

            String filename = file.getFilename();
            String extension = filename.substring(filename.lastIndexOf('.') + 1);
            String savedName = writeMediaFile(deviceSession.getUniqueId(), file.content(), extension);

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (body.containsKey("alarmTime")) {
                position.setTime(new Date(body.getJsonNumber("alarmTime").longValue() * 1000));
            }

            String latitude = body.getString("lat", null);
            String longitude = body.getString("lng", null);
            if (latitude != null && longitude != null && !latitude.isEmpty() && !longitude.isEmpty()) {
                position.setValid(true);
                position.setLatitude(Double.parseDouble(latitude));
                position.setLongitude(Double.parseDouble(longitude));
            } else {
                getLastLocation(position, position.getDeviceTime());
            }

            if (body.getString("mimeType", "").startsWith("video/")) {
                position.set(Position.KEY_VIDEO, savedName);
            } else {
                position.set(Position.KEY_IMAGE, savedName);
            }

            for (String key : List.of("businessType", "eventType", "instructionId")) {
                String value = body.getString(key, null);
                if (value != null) {
                    position.set(key, value);
                }
            }

            sendJsonResponse(channel, HttpResponseStatus.OK, 200, "the high success", savedName);
            return position;

        } finally {
            decoder.destroy();
        }
    }

    private void sendJsonResponse(Channel channel, HttpResponseStatus status, int code, String message, String data) {
        if (channel != null) {
            JsonObjectBuilder builder = Json.createObjectBuilder().add("code", code).add("message", message);
            if (data != null) {
                builder.add("data", data);
            }
            ByteBuf buffer = Unpooled.copiedBuffer(builder.build().toString(), StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buffer);
            response.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.headers().add(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

}
