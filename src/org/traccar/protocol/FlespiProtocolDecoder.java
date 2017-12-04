/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.multipart.Attribute;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

public class FlespiProtocolDecoder extends BaseHttpProtocolDecoder {

    public FlespiProtocolDecoder(FlespiProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
        InterfaceHttpData data = decoder.getBodyHttpData("data");
        if (data.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        Attribute attribute = (Attribute) data;
        String value = attribute.getValue();
        JsonArray result = Json.createReader(new StringReader(value)).readArray();
        List<Position> positions = new LinkedList<>();
        for (int i = 0; i < result.size(); i++) {
            JsonObject message = result.getJsonObject(i);
            JsonString ident = message.getJsonString("ident");
            if (ident == null) {
                continue;
            }
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, ident.getString());
            if (deviceSession == null) {
                continue;
            }
            Position position = new Position();
            position.setDeviceId(deviceSession.getDeviceId());
            position.setProtocol(getProtocolName());
            decodePosition(message, position);
            positions.add(position);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return positions;
    }

    private void decodePosition(JsonObject object, Position position) {
        // store all params in position
        Set<Map.Entry<String, JsonValue>> params = object.entrySet();
        Iterator<Map.Entry<String, JsonValue>> it = params.iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonValue> param = it.next();
            String paramName = param.getKey();
            JsonValue paramValue = param.getValue();
            if ("timestamp".equals(paramName)) {
                Date deviceTime = new Date((long) (((JsonNumber) paramValue).doubleValue() * 1000));
                position.setTime(deviceTime);
            } else if (paramName.startsWith("position.")) {
                savePosition(paramName, paramValue, position);
            } else if (paramName.matches("din#[\\d]{1,2}")) {
                saveInput(paramName, paramValue, position);
            } else if (paramName.matches("dout#[\\d]{1,2}")) {
                saveOutput(paramName, paramValue, position);
            } else if (saveAlarm(paramName, paramValue, position) == 0
                    && saveParam(paramName, paramValue, position) == 0) {
                saveUnknownParam(param.getKey(), param.getValue(), position);
            }
        }
        if (position.getLatitude() == 0 && position.getLongitude() == 0) {
            getLastLocation(position, position.getDeviceTime());
        }
    }

    private void savePosition(String name, JsonValue value, Position position) {
        switch (name) {
            case "position.latitude":
                position.setLatitude(((JsonNumber) value).doubleValue());
                break;
            case "position.longitude":
                position.setLongitude(((JsonNumber) value).doubleValue());
                break;
            case "position.speed":
                position.setSpeed(((JsonNumber) value).doubleValue());
                break;
            case "position.direction":
                position.setCourse(((JsonNumber) value).doubleValue());
                break;
            case "position.altitude":
                position.setAltitude(((JsonNumber) value).doubleValue());
                break;
            case "position.satellites":
                position.set(Position.KEY_SATELLITES, ((JsonNumber) value).intValue());
                break;
            case "position.valid":
                position.setValid(value == JsonValue.TRUE);
                break;
            case "position.hdop":
                position.set(Position.KEY_HDOP, ((JsonNumber) value).doubleValue());
                break;
            case "position.pdop":
                position.set(Position.KEY_PDOP, ((JsonNumber) value).doubleValue());
                break;
            default:
                saveUnknownParam(name, value, position);
                break;
        }
    }

    private int saveParam(String name, JsonValue value, Position position) {
        if (name == null) {
            return 0;
        }
        if (name.equals("gps.vehicle.mileage") || name.startsWith("gps.vehicle.mileage#")) {
            position.set(Position.KEY_ODOMETER, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("external.powersource.voltage") || name.startsWith("external.powersource.voltage#")) {
            position.set(Position.KEY_POWER, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("battery.voltage") || name.startsWith("battery.voltage#")) {
            position.set(Position.KEY_BATTERY, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("fuel.level") || name.equals("can.fuel.level")
                || name.startsWith("fuel.level#") || name.startsWith("can.fuel.level#")) {
            position.set(Position.KEY_FUEL_LEVEL, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("engine.rpm") || name.equals("can.engine.rpm")
                || name.startsWith("engine.rpm#") || name.startsWith("can.engine.rpm#")) {
            position.set(Position.KEY_RPM, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("can.engine.temperature") || name.startsWith("can.engine.temperature#")) {
            int seqNum = 0;
            if (name.contains("#")) {
                try {
                    seqNum = Integer.parseInt(name.replaceAll("can\\.engine\\.temperature#", ""));
                } catch (NumberFormatException e) {
                    seqNum = 0;
                }
            }
            position.set(Position.PREFIX_TEMP + seqNum, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("engine.ignition.status")) {
            position.set(Position.KEY_IGNITION, value == JsonValue.TRUE);
            return 1;
        }
        if (name.equals("movement.status")) {
            position.set(Position.KEY_MOTION, value == JsonValue.TRUE);
            return 1;
        }
        if (name.equals("device.temperature")) {
            position.set(Position.KEY_DEVICE_TEMP, ((JsonNumber) value).doubleValue());
            return 1;
        }
        if (name.equals("ibutton.code")) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, ((JsonString) value).getString());
            return 1;
        }
        if (name.equals("vehicle.vin")) {
            position.set(Position.KEY_VIN, ((JsonString) value).getString());
            return 1;
        }
        return 0;
    }

    private int saveAlarm(String name, JsonValue value, Position position) {
        if ("alarm.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
            return 1;
        }
        if ("towing.event.trigger".equals(name) || "towing.alarm.status".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_TOW);
            }
            return 1;
        }
        if ("geofence.event.enter".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);
            }
            return 1;
        }
        if ("geofence.event.exit".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_GEOFENCE_EXIT);
            }
            return 1;
        }
        if ("shock.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_SHOCK);
            }
            return 1;
        }
        if ("overspeeding.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_OVERSPEED);
            }
            return 1;
        }
        if ("harsh.acceleration.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_ACCELERATION);
            }
            return 1;
        }
        if ("harsh.braking.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_BRAKING);
            }
            return 1;
        }
        if ("harsh.cornering.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_CORNERING);
            }
            return 1;
        }
        if ("gnss.antenna.cut.status".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_GPS_ANTENNA_CUT);
            }
            return 1;
        }
        if ("gsm.jamming.event.trigger".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
            }
            return 1;
        }
        if ("hood.open.status".equals(name)) {
            if (value == JsonValue.TRUE) {
                position.set(Position.KEY_ALARM, Position.ALARM_BONNET);
            }
            return 1;
        }
        return 0;
    }

    private void saveInput(String name, JsonValue value, Position position) {
        if (value != JsonValue.TRUE) {
            return;
        }
        int in = Integer.parseInt(name.replaceAll("^din#", ""));
        if (in > 32 || in < 1) {
            return;
        }
        if (position.getInteger(Position.KEY_INPUT) == 0) {
            position.set(Position.KEY_INPUT, 1 << (in - 1));
        } else {
            position.set(Position.KEY_INPUT, (position.getInteger(Position.KEY_INPUT) | 1 << (in - 1)));
        }
    }

    private void saveOutput(String name, JsonValue value, Position position) {
        if (value != JsonValue.TRUE) {
            return;
        }
        int out = Integer.parseInt(name.replaceAll("^dout#", ""));
        if (out > 32 || out < 1) {
            return;
        }
        if (position.getInteger(Position.KEY_OUTPUT) == 0) {
            position.set(Position.KEY_OUTPUT, 1 << (out - 1));
        } else {
            position.set(Position.KEY_OUTPUT, (position.getInteger(Position.KEY_OUTPUT) | 1 << (out - 1)));
        }
    }

    private void saveUnknownParam(String name, JsonValue value, Position position) {
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
