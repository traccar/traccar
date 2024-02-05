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

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.kafka.common.utils.ByteBufferInputStream;
import org.traccar.BaseMqttProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class PuiProtocolDecoder extends BaseMqttProtocolDecoder {

    public PuiProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(DeviceSession deviceSession, MqttPublishMessage message) throws Exception {

        JsonObject json;
        try (ByteBufferInputStream inputStream = new ByteBufferInputStream(message.payload().nioBuffer())) {
            json = Json.createReader(inputStream).readObject();
        }

        String type = json.getString("rpt");
        switch (type) {
            case "hf":
            case "loc":
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setValid(true);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                position.setTime(dateFormat.parse(json.getString("ts")));

                JsonObject location = json.getJsonObject("location");
                position.setLatitude(location.getJsonNumber("lat").doubleValue());
                position.setLongitude(location.getJsonNumber("lon").doubleValue());

                position.setCourse(json.getInt("bear"));
                position.setSpeed(UnitsConverter.knotsFromCps(json.getInt("spd")));

                position.set(Position.KEY_IGNITION, json.getString("ign").equals("on"));

                return position;

            default:
                return null;
        }
    }

}
