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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Optional;

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

            if (payload.getInt("gnss.fix") > 0) {
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
                Optional.ofNullable(payload.getJsonNumber("gnss.altitude"))
                        .map(JsonNumber::doubleValue).ifPresent(position::setAltitude);
                Optional.ofNullable(payload.getJsonNumber("gnss.speed"))
                        .map(JsonNumber::doubleValue).ifPresent(position::setSpeed);
                Optional.ofNullable(payload.getJsonNumber("gnss.heading"))
                        .map(JsonNumber::doubleValue).ifPresent(position::setCourse);
                Optional.ofNullable(payload.getJsonNumber("gnss.num_sv"))
                        .map(JsonNumber::intValue).ifPresent(value -> position.set(Position.KEY_SATELLITES, value));
                Optional.ofNullable(payload.getJsonNumber("gnss.hdop"))
                        .map(JsonNumber::doubleValue).ifPresent(value -> position.set(Position.KEY_HDOP, value));
            } else {
                position.setValid(false);
                Optional.ofNullable(payload.getJsonNumber("gnss.num_sv"))
                        .map(JsonNumber::intValue).ifPresent(value -> position.set(Position.KEY_SATELLITES, value));
                position.setTime(new Date());
            }

        } else if (topic.contains("/cellular1/")) {

            getLastLocation(position, new Date(payload.getInt("modem1.ts") * 1000L));

            Optional.ofNullable(payload.getString("modem1.imei"))
                    .ifPresent(value -> position.set("imei", value));
            Optional.ofNullable(payload.getString("modem1.imsi"))
                    .ifPresent(value -> position.set("imsi", value));
            Optional.ofNullable(payload.getString("modem1.iccid"))
                    .ifPresent(value -> position.set(Position.KEY_ICCID, value));

            String operator = payload.getString("modem1.operator");
            if (!operator.isEmpty()) {
                CellTower cellTower = CellTower.from(
                        Integer.parseInt(operator.substring(0, 3)),
                        Integer.parseInt(operator.substring(3)),
                        Integer.parseInt(payload.getString("modem1.lac"), 16),
                        Integer.parseInt(payload.getString("modem1.cell_id"), 16));

                if (payload.containsKey("modem1.rsrp")) {
                    cellTower.setSignalStrength(payload.getInt("modem1.rsrp"));
                } else if (payload.containsKey("modem1.rssi")) {
                    cellTower.setSignalStrength(payload.getInt("modem1.rssi"));
                }

                switch (payload.getInt("modem1.network")) {
                    case 1:
                        cellTower.setRadioType("gsm");
                        break;
                    case 2:
                        cellTower.setRadioType("wcdma");
                        break;
                    case 3:
                        cellTower.setRadioType("lte");
                        break;
                    default:
                        break;
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
            Optional.ofNullable(payload.getJsonNumber("motion.ax"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("ax", value));
            Optional.ofNullable(payload.getJsonNumber("motion.ay"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("ay", value));
            Optional.ofNullable(payload.getJsonNumber("motion.az"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("az", value));
            Optional.ofNullable(payload.getJsonNumber("motion.gx"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("gx", value));
            Optional.ofNullable(payload.getJsonNumber("motion.gy"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("gy", value));
            Optional.ofNullable(payload.getJsonNumber("motion.gz"))
                    .map(JsonNumber::doubleValue).ifPresent(value -> position.set("gz", value));
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
