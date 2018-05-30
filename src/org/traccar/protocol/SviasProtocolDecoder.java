/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.model.Position;
import java.text.ParseException;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.traccar.helper.UnitsConverter;

public class SviasProtocolDecoder extends BaseProtocolDecoder {

    public SviasProtocolDecoder(SviasProtocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, String prefix) {
        if (channel != null) {
            channel.write(prefix);
        }
    }

    private double convertCoordinates(long v) {
        float a = (float) v / 1.0E7F;
        int b = (int) (v / 10000000L);
        float c = a - b;
        float d = (float) (c * 1.6666666666666667D);
        int e = (int) (v / 10000000L);
        float f = d + e;

        return Double.valueOf(f);

    }

    private Position decodePosition(Channel channel, SocketAddress remoteAddress, String substring)
            throws ParseException {
        int index = 0;

        String[] values = substring.split(",");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[3]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        String versionHw = values[index++].replaceAll("[^0-9]", "");
        String versionSw = values[index++];

        position.set(Position.KEY_INDEX, Integer.valueOf(values[index++]));

        String imei = values[index++];
        position.set(Position.KEY_TYPE, values[index++]);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = String.format("%06d", Integer.parseInt(values[index++]));
        String time = String.format("%06d", Integer.parseInt(values[index++]));

        position.setTime(dateFormat.parse(date + time));

        position.setLatitude(convertCoordinates(Long.valueOf(values[index++])));
        position.setLongitude(convertCoordinates(Long.valueOf(values[index++])));

        position.setSpeed(UnitsConverter.knotsFromKph(Integer.valueOf(values[index++]) / 100));
        position.setCourse(Integer.valueOf(values[index++]) / 100);

        position.set(Position.KEY_ODOMETER, Integer.valueOf(values[index++]));

        String input = new StringBuilder(String.format("%08d",
                Integer.parseInt(values[index++]))).reverse().toString();

        String output = new StringBuilder(String.format("%08d",
                Integer.parseInt(values[index++]))).reverse().toString();

        if (input.substring(0).equals("1")) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }
        position.set(Position.KEY_IGNITION, input.substring(4).equals("1"));

        position.setValid(output.substring(0).equals("1"));

        position.set(Position.KEY_POWER, Integer.valueOf(values[index++]) / 100);

        position.set(Position.KEY_BATTERY_LEVEL, Double.parseDouble(values[index++]));

        position.set(Position.KEY_RSSI, Integer.valueOf(values[index++]));

        if (values.length == 22) {
            String driverUniqueId = values[index++];
            if (!driverUniqueId.isEmpty()) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, driverUniqueId);
            }
        }

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.contains(":")) {

            String[] values = sentence.substring(1).split(":");

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[1]);

            if (deviceSession != null) {
                sendResponse(channel, "@");
            }

        } else {

            Position position = decodePosition(channel, remoteAddress, sentence.substring(1));

            if (position != null) {
                sendResponse(channel, "@");
                return position;
            }
        }

        return null;

    }

}
