/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.helper.BufferUtil;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SigfoxProtocolDecoder extends BaseHttpProtocolDecoder {

    public SigfoxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private boolean jsonContains(JsonObject json, String key) {
        if (json.containsKey(key)) {
            JsonValue value = json.get(key);
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return !((JsonString) value).getString().equals("null");

            } else {
                return true;
            }
        }
        return false;
    }

    private boolean getJsonBoolean(JsonObject json, String key) {
        JsonValue value = json.get(key);
        if (value != null) {
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return Boolean.parseBoolean(((JsonString) value).getString());
            } else {
                return value.getValueType() == JsonValue.ValueType.TRUE;
            }
        }
        return false;
    }

    private int getJsonInt(JsonObject json, String key) {
        JsonValue value = json.get(key);
        if (value != null) {
            if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return ((JsonNumber) value).intValue();
            } else if (value.getValueType() == JsonValue.ValueType.STRING) {
                return Integer.parseInt(((JsonString) value).getString());
            }
        }
        return 0;
    }

    private double getJsonDouble(JsonObject json, String key) {
        JsonValue value = json.get(key);
        if (value != null) {
            if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return ((JsonNumber) value).doubleValue();
            } else if (value.getValueType() == JsonValue.ValueType.STRING) {
                return Double.parseDouble(((JsonString) value).getString());
            }
        }
        return 0;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        String content = request.content().toString(StandardCharsets.UTF_8);
        if (!content.startsWith("{")) {
            content = URLDecoder.decode(content.split("=")[0], "UTF-8");
        }
        JsonObject json = Json.createReader(new StringReader(content)).readObject();

        String deviceId;
        if (json.containsKey("device")) {
            deviceId = json.getString("device");
        } else {
            deviceId = json.getString("deviceId");
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (jsonContains(json, "time")) {
            position.setTime(new Date(getJsonInt(json, "time") * 1000L));
        } else if (jsonContains(json, "positionTime")) {
            position.setTime(new Date(getJsonInt(json, "positionTime") * 1000L));
        } else {
            position.setTime(new Date());
        }

        if (jsonContains(json, "lastSeen")) {
            position.setDeviceTime(new Date(getJsonInt(json, "lastSeen") * 1000L));
        }

        if (jsonContains(json, "location")
                || jsonContains(json, "lat") && jsonContains(json, "lng") && !jsonContains(json, "data")
                || jsonContains(json, "latitude") && jsonContains(json, "longitude") && !jsonContains(json, "data")) {

            JsonObject location;
            if (jsonContains(json, "location")) {
                location = json.getJsonObject("location");
            } else {
                location = json;
            }

            position.setValid(true);
            position.setLatitude(getJsonDouble(location, jsonContains(location, "lat") ? "lat" : "latitude"));
            position.setLongitude(getJsonDouble(location, jsonContains(location, "lng") ? "lng" : "longitude"));

        } else if (jsonContains(json, "data")) {

            ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(json.getString("data")));
            try {
                int event = buf.readUnsignedByte();
                if (event == 0x0f || event == 0x1f) {

                    position.setValid(event >> 4 > 0);
                    position.setLatitude(BufferUtil.readSignedMagnitudeInt(buf) * 0.000001);
                    position.setLongitude(BufferUtil.readSignedMagnitudeInt(buf) * 0.000001);

                    position.set(Position.KEY_BATTERY, (int) buf.readUnsignedByte());

                } else if (event >> 4 <= 3 && buf.writerIndex() == 12) {

                    if (BitUtil.to(event, 4) == 0) {
                        position.setValid(true);
                        position.setLatitude(buf.readIntLE() * 0.0000001);
                        position.setLongitude(buf.readIntLE() * 0.0000001);
                        position.setCourse(buf.readUnsignedByte() * 2);
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

                        position.set(Position.KEY_BATTERY, buf.readUnsignedByte() * 0.025);
                    } else {
                        return null;
                    }

                }
            } finally {
                buf.release();
            }

        } else if (jsonContains(json, "payload")) {

            ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(json.getString("payload")));
            try {
                int event = buf.readUnsignedByte();
                position.set(Position.KEY_EVENT, event);
                if (event == 0x22 || event == 0x62) {
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                }

                while (buf.isReadable()) {
                    int type = buf.readUnsignedByte();
                    switch (type) {
                        case 0x01:
                            position.setValid(true);
                            position.setLatitude(buf.readMedium());
                            position.setLongitude(buf.readMedium());
                            break;
                        case 0x02:
                            position.setValid(true);
                            position.setLatitude(buf.readFloat());
                            position.setLongitude(buf.readFloat());
                            break;
                        case 0x03:
                            position.set(Position.PREFIX_TEMP + 1, buf.readByte() * 0.5);
                            break;
                        case 0x04:
                            position.set(Position.KEY_BATTERY, buf.readUnsignedByte() * 0.1);
                            break;
                        case 0x05:
                            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                            break;
                        case 0x06:
                            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                            position.setNetwork(new Network(WifiAccessPoint.from(
                                    mac.substring(0, mac.length() - 1), buf.readUnsignedByte())));
                            break;
                        case 0x07:
                            buf.skipBytes(10); // wifi extended
                            break;
                        case 0x08:
                            buf.skipBytes(6); // accelerometer
                            break;
                        case 0x09:
                            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                            break;
                        default:
                            buf.readUnsignedByte(); // fence number
                            break;
                    }
                }
            } finally {
                buf.release();
            }

        }

        if (position.getLatitude() == 0 && position.getLongitude() == 0) {
            getLastLocation(position, position.getDeviceTime());
        }

        if (jsonContains(json, "moving")) {
            position.set(Position.KEY_MOTION, getJsonBoolean(json, "moving"));
        }
        if (jsonContains(json, "magStatus")) {
            position.set(Position.KEY_BLOCKED, getJsonBoolean(json, "magStatus"));
        }
        if (jsonContains(json, "temperature")) {
            position.set(Position.KEY_DEVICE_TEMP, getJsonDouble(json, "temperature"));
        }
        if (jsonContains(json, "rssi")) {
            position.set(Position.KEY_RSSI, getJsonDouble(json, "rssi"));
        }
        if (jsonContains(json, "seqNumber")) {
            position.set(Position.KEY_INDEX, getJsonInt(json, "seqNumber"));
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return position;
    }

}
