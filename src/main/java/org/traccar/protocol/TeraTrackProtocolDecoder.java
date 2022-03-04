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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TeraTrackProtocolDecoder extends BaseHttpProtocolDecoder {

    public TeraTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        JsonObject json = Json.createReader(
                new StringReader(request.content().toString(StandardCharsets.US_ASCII))).readObject();

        String deviceId = json.getString("MDeviceID");
        String imei = json.getString("IMEI");
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId, imei);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.NOT_FOUND);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(json.getString("DateTime")));

        position.setValid(true);

        double latitude = Double.parseDouble(json.getString("Latitude"));
        position.setLatitude(json.getString("LatitudeState").equals("0") ? -latitude : latitude);
        double longitude = Double.parseDouble(json.getString("Longitude"));
        position.setLongitude(json.getString("LongitudeState").equals("0") ? -longitude : longitude);

        position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(json.getString("Speed"))));

        position.set(Position.KEY_ODOMETER, Integer.parseInt(json.getString("Mileage")));

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

}
