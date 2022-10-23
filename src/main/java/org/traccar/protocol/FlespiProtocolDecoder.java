/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlespiProtocolDecoder extends BaseHttpProtocolDecoder {

    public FlespiProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        JsonArray result = Json.createReader(new StringReader(request.content().toString(StandardCharsets.UTF_8)))
                .readArray();
        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < result.size(); i++) {
            JsonObject message = result.getJsonObject(i);
            JsonString identifier = message.getJsonString("ident");
            if (identifier == null) {
                continue;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, identifier.getString());
            if (deviceSession == null) {
                continue;
            }
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setValid(true);
            decodePosition(message, position);
            positions.add(position);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return positions;
    }

    private void decodePosition(JsonObject object, Position position) {
        for (Map.Entry<String, JsonValue> param : object.entrySet()) {
            String paramName = param.getKey();
            JsonValue paramValue = param.getValue();
            int index = -1;
            if (paramName.contains("#")) {
                String[] parts = paramName.split("#");
                paramName = parts[0];
                index = Integer.parseInt(parts[1]);
            }
            if (!decodeParam(paramName, index, paramValue, position)) {
                decodeUnknownParam(param.getKey(), param.getValue(), position);
            }
        }
        if (position.getLatitude() == 0 && position.getLongitude() == 0) {
            getLastLocation(position, position.getDeviceTime());
        }
    }

    private boolean decodeParam(String name, int index, JsonValue value, Position position) {
        switch (name) {
            case "timestamp":
                position.setTime(new Date(((JsonNumber) value).longValue() * 1000));
                return true;
            case "position.latitude":
                position.setLatitude(((JsonNumber) value).doubleValue());
                return true;
            case "position.longitude":
                position.setLongitude(((JsonNumber) value).doubleValue());
                return true;
            case "position.speed":
                position.setSpeed(UnitsConverter.knotsFromKph(((JsonNumber) value).doubleValue()));
                return true;
            case "position.direction":
                position.setCourse(((JsonNumber) value).doubleValue());
                return true;
            case "position.altitude":
                position.setAltitude(((JsonNumber) value).doubleValue());
                return true;
            case "position.satellites":
                position.set(Position.KEY_SATELLITES, ((JsonNumber) value).intValue());
                return true;
            case "position.valid":
                position.setValid(value == JsonValue.TRUE);
                return true;
            case "position.hdop":
                position.set(Position.KEY_HDOP, ((JsonNumber) value).doubleValue());
                return true;
            case "position.pdop":
                position.set(Position.KEY_PDOP, ((JsonNumber) value).doubleValue());
                return true;
            case "din":
            case "dout":
                if (name.equals("din")) {
                    position.set(Position.KEY_INPUT, ((JsonNumber) value).intValue());
                } else {
                    position.set(Position.KEY_OUTPUT, ((JsonNumber) value).intValue());
                }
                return true;
            case "gps.vehicle.mileage":
                position.set(Position.KEY_ODOMETER, ((JsonNumber) value).doubleValue());
                return true;
            case "external.powersource.voltage":
                position.set(Position.KEY_POWER, ((JsonNumber) value).doubleValue());
                return true;
            case "battery.voltage":
                position.set(Position.KEY_BATTERY, ((JsonNumber) value).doubleValue());
                return true;
            case "fuel.level":
            case "can.fuel.level":
                position.set(Position.KEY_FUEL_LEVEL, ((JsonNumber) value).doubleValue());
                return true;
            case "engine.rpm":
            case "can.engine.rpm":
                position.set(Position.KEY_RPM, ((JsonNumber) value).doubleValue());
                return true;
            case "can.engine.temperature":
                position.set(Position.PREFIX_TEMP + Math.max(index, 0), ((JsonNumber) value).doubleValue());
                return true;
            case "engine.ignition.status":
                position.set(Position.KEY_IGNITION, value == JsonValue.TRUE);
                return true;
            case "movement.status":
                position.set(Position.KEY_MOTION, value == JsonValue.TRUE);
                return true;
            case "device.temperature":
                position.set(Position.KEY_DEVICE_TEMP, ((JsonNumber) value).doubleValue());
                return true;
            case "ibutton.code":
                position.set(Position.KEY_DRIVER_UNIQUE_ID, ((JsonString) value).getString());
                return true;
            case "vehicle.vin":
                position.set(Position.KEY_VIN, ((JsonString) value).getString());
                return true;
            case "alarm.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                }
                return true;
            case "towing.event.trigger":
            case "towing.alarm.status":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_TOW);
                }
                return true;
            case "geofence.event.enter":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);
                }
                return true;
            case "geofence.event.exit":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
                }
                return true;
            case "shock.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_VIBRATION);
                }
                return true;
            case "overspeeding.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
                }
                return true;
            case "harsh.acceleration.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
                }
                return true;
            case "harsh.braking.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
                }
                return true;
            case "harsh.cornering.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
                }
                return true;
            case "gnss.antenna.cut.status":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GPS_ANTENNA_CUT);
                }
                return true;
            case "gsm.jamming.event.trigger":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
                }
                return true;
            case "hood.open.status":
                if (value == JsonValue.TRUE) {
                    position.set(Position.KEY_ALARM, Position.ALARM_BONNET);
                }
                return true;
            case "custom.wln_accel_max":
                position.set("maxAcceleration", ((JsonNumber) value).doubleValue());
                return true;
            case "custom.wln_brk_max":
                position.set("maxBraking", ((JsonNumber) value).doubleValue());
                return true;
            case "custom.wln_crn_max":
                position.set("maxCornering", ((JsonNumber) value).doubleValue());
                return true;
            default:
                return false;
        }
    }

    private void decodeUnknownParam(String name, JsonValue value, Position position) {
        if (value instanceof JsonNumber) {
            if (((JsonNumber) value).isIntegral()) {
                position.set(name, ((JsonNumber) value).longValue());
            } else {
                position.set(name, ((JsonNumber) value).doubleValue());
            }
            position.set(name, ((JsonNumber) value).doubleValue());
        } else if (value instanceof JsonString) {
            position.set(name, ((JsonString) value).getString());
        } else if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
            position.set(name, value == JsonValue.TRUE);
        }
    }

}
