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
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import jakarta.json.Json;
import jakarta.json.JsonObject;
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

        if (topic.contains("/gnss/")) {

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

            position.setValid(payload.getInt("gnss.fix") > 0);
            position.setAltitude(payload.getJsonNumber("gnss.altitude").doubleValue());
            position.setSpeed(payload.getJsonNumber("gnss.speed").doubleValue());
            position.setCourse(payload.getJsonNumber("gnss.heading").doubleValue());

            position.set(Position.KEY_SATELLITES, payload.getInt("gnss.num_sv"));
            position.set(Position.KEY_HDOP, payload.getJsonNumber("gnss.hdop").doubleValue());

        } else if (topic.contains("/cellular1/")) {

            getLastLocation(position, new Date(payload.getInt("modem1.ts") * 1000L));

            position.set("imei", payload.getString("modem1.imei"));
            position.set("imsi", payload.getString("modem1.imsi"));
            position.set(Position.KEY_ICCID, payload.getString("modem1.iccid"));

            String operator = payload.getString("modem1.operator");
            if (!operator.isEmpty()) {
                CellTower cellTower = CellTower.from(
                        Integer.parseInt(operator.substring(0, 3)),
                        Integer.parseInt(operator.substring(3)),
                        Integer.parseInt(payload.getString("modem1.lac"), 16),
                        Integer.parseInt(payload.getString("modem1.cell_id"), 16),
                        payload.getInt("modem1.rssi"));
                switch (payload.getInt("modem1.network")) {
                    case 1 -> cellTower.setRadioType("gsm");
                    case 2 -> cellTower.setRadioType("wcdma");
                    case 3 -> cellTower.setRadioType("lte");
                }
                position.setNetwork(new Network(cellTower));
            }

        } else if (topic.contains("/obd/")) {

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

        } else if (topic.contains("/motion/")) {

            getLastLocation(position, new Date(payload.getInt("motion.ts") * 1000L));

            position.set("ax", payload.getJsonNumber("motion.ax").doubleValue());
            position.set("ay", payload.getJsonNumber("motion.ay").doubleValue());
            position.set("az", payload.getJsonNumber("motion.az").doubleValue());
            position.set("gx", payload.getJsonNumber("motion.gx").doubleValue());
            position.set("gy", payload.getJsonNumber("motion.gy").doubleValue());
            position.set("gz", payload.getJsonNumber("motion.gz").doubleValue());

        } else if (topic.contains("/io/")) {

            getLastLocation(position, new Date(payload.getInt("io.ts") * 1000L));

            if (payload.containsKey("io.IGT")) {
                position.set(Position.KEY_IGNITION, payload.getInt("io.IGT") > 0);
            }

            for (String key : payload.keySet()) {
                if (key.startsWith("io.AI")) {
                    position.set(Position.PREFIX_ADC + key.substring(5), payload.getJsonNumber(key).doubleValue());
                } else if (key.startsWith("io.DI") && !key.endsWith("_pullup")) {
                    position.set(Position.PREFIX_IN + key.substring(5), payload.getInt(key) > 0);
                } else if (key.startsWith("io.DO")) {
                    position.set(Position.PREFIX_OUT + key.substring(5), payload.getInt(key) > 0);
                }
            }

        } else if (topic.contains("/sysinfo/")) {

            getLastLocation(position, new Date(payload.getInt("sysinfo.ts") * 1000L));

            position.set("serial", payload.getString("sysinfo.serial_number"));
            position.set(Position.KEY_VERSION_FW, payload.getString("sysinfo.firmware_version"));

        } else {

            return null;

        }

        return position;
    }

}
