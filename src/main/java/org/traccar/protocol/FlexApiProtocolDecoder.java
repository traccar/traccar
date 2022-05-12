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
        position.set(Position.KEY_ORIGINAL, message);
        position.setDeviceId(deviceSession.getDeviceId());

        JsonObject payload = root.getJsonObject("payload");

        if (topic.contains("/gnss/")) {
            parseGnss(position, payload);
        } else if (topic.contains("/cellular1/")) {

            getLastLocation(position, new Date(payload.getInt("modem1.ts") * 1000L));

            if (payload.containsKey("modem1.imei")) {
                position.set("imei", payload.getString("modem1.imei"));
            }
            if (payload.containsKey("modem1.rssi")) {
                position.set(Position.KEY_RSSI, payload.getInt("modem1.rssi"));
            }
            if (payload.containsKey("modem1.imsi")) {
                position.set("imsi", payload.getString("modem1.imsi"));
            }
            if (payload.containsKey("modem1.iccid")) {
                position.set(Position.KEY_ICCID, payload.getString("modem1.iccid"));
            }


            String operator = payload.getString("modem1.operator");
            if (!operator.isEmpty()) {
                CellTower cellTower = CellTower.from(
                        Integer.parseInt(operator.substring(0, 3)),
                        Integer.parseInt(operator.substring(3)),
                        Integer.parseInt(payload.getString("modem1.lac"), 16),
                        Integer.parseInt(payload.getString("modem1.cell_id"), 16));

                if (payload.containsKey("modem1.rsrp")) {
                    cellTower.setSignalStrength(payload.getInt("modem1.rsrp"));
                }
                if (payload.containsKey("modem1.rssi")) {
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

            parseObd(position, payload);

        } else if (topic.contains("/motion/")) {

            getLastLocation(position, new Date(payload.getInt("motion.ts") * 1000L));
            if (payload.containsKey("motion.ax")) {
                position.set("ax", payload.getJsonNumber("motion.ax").doubleValue());
            }
            if (payload.containsKey("motion.ay")) {
                position.set("ay", payload.getJsonNumber("motion.ay").doubleValue());
            }
            if (payload.containsKey("motion.az")) {
                position.set("az", payload.getJsonNumber("motion.az").doubleValue());
            }
            if (payload.containsKey("motion.gx")) {
                position.set("gx", payload.getJsonNumber("motion.gx").doubleValue());
            }
            if (payload.containsKey("motion.gy")) {
                position.set("gy", payload.getJsonNumber("motion.gy").doubleValue());
            }
            if (payload.containsKey("motion.gz")) {
                position.set("gz", payload.getJsonNumber("motion.gz").doubleValue());
            }
        } else if (topic.contains("/io/") || (topic.contains("/event/notice") && "DI_CHG".equals(payload.getString("type")))) {

            getLastLocation(position, new Date(payload.getInt("io.ts") * 1000L));

            if (payload.containsKey("io.IGT")) {
                position.set(Position.KEY_IGNITION, payload.getInt("io.IGT") > 0);
            }

            parseIO(position, payload);

        } else if (topic.contains("/sysinfo/")) {

            getLastLocation(position, new Date(payload.getInt("sysinfo.ts") * 1000L));

            position.set("serial", payload.getString("sysinfo.serial_number"));
            position.set(Position.KEY_VERSION_FW, payload.getString("sysinfo.firmware_version"));

        } else if (topic.contains("/summary/")) {
            parseIO(position, payload);
            parseGnss(position, payload);
            parseObd(position, payload);

        }

        return position;
    }

    private void parseObd(Position position, JsonObject payload) {
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
    }

    private void parseGnss(Position position, JsonObject payload) {
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
            if (payload.containsKey("gnss.altitude")) {
                position.setAltitude(payload.getJsonNumber("gnss.altitude").doubleValue());
            }
            if (payload.containsKey("gnss.speed")) {
                position.setSpeed(payload.getJsonNumber("gnss.speed").doubleValue());
            }
            if (payload.containsKey("gnss.heading")) {
                position.setCourse(payload.getJsonNumber("gnss.heading").doubleValue());
            }
            if (payload.containsKey("gnss.num_sv")) {
                position.set(Position.KEY_SATELLITES, payload.getJsonNumber("gnss.num_sv").doubleValue());
            }
            if (payload.containsKey("gnss.hdop")) {
                position.set(Position.KEY_HDOP, payload.getJsonNumber("gnss.hdop").doubleValue());
            }
        } else {
            position.setValid(false);
            if (payload.containsKey("gnss.num_sv")) {
                position.set(Position.KEY_SATELLITES, payload.getJsonNumber("gnss.num_sv").intValue());
            }
            position.setTime(new Date());
        }
    }

    private void parseIO(Position position, JsonObject payload) {
        for (String key : payload.keySet()) {
            if (key.startsWith("io.AI")) {
                position.set(Position.PREFIX_ADC + key.substring(5), payload.getJsonNumber(key).doubleValue());
            } else if (key.startsWith("io.DI") && !key.endsWith("_pullup")) {
                position.set(Position.PREFIX_IN + key.substring(5), payload.getInt(key) > 0);
            } else if (key.startsWith("io.DO")) {
                position.set(Position.PREFIX_OUT + key.substring(5), payload.getInt(key) > 0);
            }
        }
    }

}
