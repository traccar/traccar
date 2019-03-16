/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class DmtHttpProtocolDecoder extends BaseHttpProtocolDecoder {

    public DmtHttpProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;
        JsonObject root = Json.createReader(
                new StringReader(request.content().toString(StandardCharsets.US_ASCII))).readObject();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, root.getString("IMEI"));
        if (deviceSession == null) {
            sendResponse(channel, HttpResponseStatus.BAD_REQUEST);
            return null;
        }

        List<Position> positions = new LinkedList<>();

        JsonArray records = root.getJsonArray("Records");

        for (int i = 0; i < records.size(); i++) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            JsonObject record = records.getJsonObject(i);

            position.set(Position.KEY_INDEX, record.getInt("SeqNo"));
            position.set(Position.KEY_EVENT, record.getInt("Reason"));

            position.setDeviceTime(dateFormat.parse(record.getString("DateUTC")));

            JsonArray fields = record.getJsonArray("Fields");

            for (int j = 0; j < fields.size(); j++) {
                JsonObject field = fields.getJsonObject(j);
                switch (field.getInt("FType")) {
                    case 0:
                        position.setFixTime(dateFormat.parse(field.getString("GpsUTC")));
                        position.setLatitude(field.getJsonNumber("Lat").doubleValue());
                        position.setLongitude(field.getJsonNumber("Long").doubleValue());
                        position.setAltitude(field.getInt("Alt"));
                        position.setSpeed(UnitsConverter.knotsFromCps(field.getInt("Spd")));
                        position.setCourse(field.getInt("Head"));
                        position.setAccuracy(field.getInt("PosAcc"));
                        position.setValid(field.getInt("GpsStat") > 0);
                        break;
                    case 2:
                        int input = field.getInt("DIn");
                        int output = field.getInt("DOut");

                        position.set(Position.KEY_IGNITION, BitUtil.check(input, 0));

                        position.set(Position.KEY_INPUT, input);
                        position.set(Position.KEY_OUTPUT, output);
                        position.set(Position.KEY_STATUS, field.getInt("DevStat"));
                        break;
                    case 6:
                        JsonObject adc = field.getJsonObject("AnalogueData");
                        if (adc.containsKey("1")) {
                            position.set(Position.KEY_BATTERY, adc.getInt("1") * 0.001);
                        }
                        if (adc.containsKey("2")) {
                            position.set(Position.KEY_POWER, adc.getInt("2") * 0.01);
                        }
                        if (adc.containsKey("3")) {
                            position.set(Position.KEY_DEVICE_TEMP, adc.getInt("3") * 0.01);
                        }
                        if (adc.containsKey("4")) {
                            position.set(Position.KEY_RSSI, adc.getInt("4"));
                        }
                        if (adc.containsKey("5")) {
                            position.set("solarPower", adc.getInt("5") * 0.001);
                        }
                        break;
                    default:
                        break;
                }
            }

            positions.add(position);
        }

        sendResponse(channel, HttpResponseStatus.OK);
        return positions;
    }

}
