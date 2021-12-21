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
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.util.Date;

public class FlexApiProtocolDecoder extends BaseProtocolDecoder {

    public FlexApiProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String message = (String) msg;
        JsonObject root = Json.createReader(new StringReader(message.substring(1, message.length() - 2))).readObject();

        String topic = root.getString("topic");
        String clientId = topic.substring(3, topic.indexOf('/', 3));
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, clientId);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        JsonObject payload = root.getJsonObject("payload");

        if (topic.contains("gnss")) {

            position.setValid(true);

            if (payload.containsKey("time")) {
                position.setTime(new Date(payload.getInt("time") * 1000L));
                position.setLatitude(payload.getJsonNumber("lat").doubleValue());
                position.setLongitude(payload.getJsonNumber("log").doubleValue());
            } else {
                position.setTime(new Date(payload.getInt("gnss.ts") * 1000L));
                position.setLatitude(payload.getJsonNumber("gnss.latitude").doubleValue());
                position.setLongitude(payload.getJsonNumber("gnss.longitude").doubleValue());
            }

            position.setAltitude(payload.getJsonNumber("gnss.altitude").doubleValue());
            position.setSpeed(payload.getJsonNumber("gnss.speed").doubleValue());
            position.setCourse(payload.getJsonNumber("gnss.heading").doubleValue());

            position.set(Position.KEY_SATELLITES, payload.getInt("gnss.num_sv"));

        } else if (topic.contains("obd")) {

            getLastLocation(position, new Date(payload.getInt("obd.ts") * 1000L));

            if (payload.containsKey("obd.speed")) {
                position.set(Position.KEY_OBD_SPEED, payload.getJsonNumber("obd.speed").doubleValue());
            }
            if (payload.containsKey("obd.odo")) {
                position.set(Position.KEY_OBD_ODOMETER, payload.getInt("obd.odo"));
            }
            if (payload.containsKey("obd.rpm")) {
                position.set(Position.KEY_RPM, payload.getInt("obd.rpm"));
            }
            if (payload.containsKey("obd.vin")) {
                position.set(Position.KEY_VIN, payload.getString("obd.vin"));
            }

        } else {

            return null;

        }

        return position;
    }

}
