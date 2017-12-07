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
            int index = -1;
            if (paramName.contains("#")) {
                String[] parts = paramName.split("#");
                paramName = parts[0];
                index = Integer.parseInt(parts[1]);
            }
            if (!saveParam(paramName, index, paramValue, position)) {
                saveUnknownParam(param.getKey(), param.getValue(), position);
            }
        }
        if (position.getLatitude() == 0 && position.getLongitude() == 0) {
            getLastLocation(position, position.getDeviceTime());
        }
    }

    private boolean saveParam(String name, int index, JsonValue value, Position position) {
        switch (name) {
            case "timestamp":
                Date deviceTime = new Date((long) (((JsonNumber) value).doubleValue() * 1000));
                position.setTime(deviceTime);
                return true;
            case "position.latitude":
                position.setLatitude(((JsonNumber) value).doubleValue());
                return true;
            case "position.longitude":
                position.setLongitude(((JsonNumber) value).doubleValue());
                return true;
            case "position.speed":
                position.setSpeed(((JsonNumber) value).doubleValue());
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
                String key = ("din".equals(name)) ? Position.KEY_INPUT : Position.KEY_OUTPUT;
                if (value == JsonValue.TRUE && index <= 32 && index >= 1) {
                    if (position.getInteger(key) == 0) {
                        position.set(key, 1 << (index - 1));
                    } else {
                        position.set(key, (position.getInteger(key) | 1 << (index - 1)));
                    }
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
                position.set(Position.PREFIX_TEMP + (index > 0 ? index : 0), ((JsonNumber) value).doubleValue());
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
                    position.set(Position.KEY_ALARM, Position.ALARM_SHOCK);
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
            default:
                return false;
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
