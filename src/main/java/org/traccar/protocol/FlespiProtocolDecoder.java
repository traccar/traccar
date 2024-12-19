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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
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
        return switch (name) {
            case "timestamp" -> {
                position.setTime(new Date(((JsonNumber) value).longValue() * 1000));
                yield true;
            }
            case "position.latitude" -> {
                position.setLatitude(((JsonNumber) value).doubleValue());
                yield true;
            }
            case "position.longitude" -> {
                position.setLongitude(((JsonNumber) value).doubleValue());
                yield true;
            }
            case "position.speed" -> {
                position.setSpeed(UnitsConverter.knotsFromKph(((JsonNumber) value).doubleValue()));
                yield true;
            }
            case "position.direction" -> {
                position.setCourse(((JsonNumber) value).doubleValue());
                yield true;
            }
            case "position.altitude" -> {
                position.setAltitude(((JsonNumber) value).doubleValue());
                yield true;
            }
            case "position.satellites" -> {
                position.set(Position.KEY_SATELLITES, ((JsonNumber) value).intValue());
                yield true;
            }
            case "position.valid" -> {
                position.setValid(value == JsonValue.TRUE);
                yield true;
            }
            case "position.hdop" -> {
                position.set(Position.KEY_HDOP, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "position.pdop" -> {
                position.set(Position.KEY_PDOP, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "din" -> {
                position.set(Position.KEY_INPUT, ((JsonNumber) value).intValue());
                yield true;
            }
            case "dout" -> {
                position.set(Position.KEY_OUTPUT, ((JsonNumber) value).intValue());
                yield true;
            }
            case "report.reason" -> {
                position.set(Position.KEY_EVENT, ((JsonNumber) value).intValue());
                yield true;
            }
            case "gps.vehicle.mileage" -> {
                position.set(Position.KEY_ODOMETER, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "external.powersource.voltage" -> {
                position.set(Position.KEY_POWER, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "battery.voltage" -> {
                position.set(Position.KEY_BATTERY, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "battery.level" -> {
                position.set(Position.KEY_BATTERY_LEVEL, ((JsonNumber) value).intValue());
                yield true;
            }
            case "fuel.level", "can.fuel.level" -> {
                position.set(Position.KEY_FUEL_LEVEL, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "engine.rpm", "can.engine.rpm" -> {
                position.set(Position.KEY_RPM, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "can.engine.temperature" -> {
                position.set(Position.PREFIX_TEMP + Math.max(index, 0), ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "engine.ignition.status" -> {
                position.set(Position.KEY_IGNITION, value == JsonValue.TRUE);
                yield true;
            }
            case "movement.status" -> {
                position.set(Position.KEY_MOTION, value == JsonValue.TRUE);
                yield true;
            }
            case "device.temperature" -> {
                position.set(Position.KEY_DEVICE_TEMP, ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "ibutton.code" -> {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, ((JsonString) value).getString());
                yield true;
            }
            case "vehicle.vin" -> {
                position.set(Position.KEY_VIN, ((JsonString) value).getString());
                yield true;
            }
            case "alarm.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_GENERAL);
                }
                yield true;
            }
            case "towing.event.trigger", "towing.alarm.status" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_TOW);
                }
                yield true;
            }
            case "geofence.event.enter" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_GEOFENCE_ENTER);
                }
                yield true;
            }
            case "geofence.event.exit" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_GEOFENCE_EXIT);
                }
                yield true;
            }
            case "shock.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_VIBRATION);
                }
                yield true;
            }
            case "overspeeding.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_OVERSPEED);
                }
                yield true;
            }
            case "harsh.acceleration.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_ACCELERATION);
                }
                yield true;
            }
            case "harsh.braking.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_BRAKING);
                }
                yield true;
            }
            case "harsh.cornering.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_CORNERING);
                }
                yield true;
            }
            case "gnss.antenna.cut.status" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_GPS_ANTENNA_CUT);
                }
                yield true;
            }
            case "gsm.jamming.event.trigger" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_JAMMING);
                }
                yield true;
            }
            case "hood.open.status" -> {
                if (value == JsonValue.TRUE) {
                    position.addAlarm(Position.ALARM_BONNET);
                }
                yield true;
            }
            case "custom.wln_accel_max" -> {
                position.set("maxAcceleration", ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "custom.wln_brk_max" -> {
                position.set("maxBraking", ((JsonNumber) value).doubleValue());
                yield true;
            }
            case "custom.wln_crn_max" -> {
                position.set("maxCornering", ((JsonNumber) value).doubleValue());
                yield true;
            }
            default -> false;
        };
    }

    private void decodeUnknownParam(String name, JsonValue value, Position position) {
        if (value instanceof JsonNumber jsonNumber) {
            if (jsonNumber.isIntegral()) {
                position.set(name, jsonNumber.longValue());
            } else {
                position.set(name, jsonNumber.doubleValue());
            }
            position.set(name, jsonNumber.doubleValue());
        } else if (value instanceof JsonString jsonString) {
            position.set(name, jsonString.getString());
        } else if (value == JsonValue.TRUE || value == JsonValue.FALSE) {
            position.set(name, value == JsonValue.TRUE);
        }
    }

}
