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
import org.traccar.helper.Log;
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

    private String zeros(String texto, Integer valor) {
        String aux = "";
        for (int i = 0; i < valor; i++) {
            aux += '0';
        }
        return aux + texto;
    }

    public String decimalToBinary(int valor) {

        String bin = Integer.toString(valor, 2);
        return bin;
    }

    private double convert2decimal(long v) {
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

        position.set(Position.KEY_VERSION_HW, values[index++].replace("[", ""));

        String swOrAlt = values[index++];
        position.set(Position.KEY_VERSION_HW, swOrAlt);
        position.setAltitude(Double.parseDouble(swOrAlt));

        position.set(Position.KEY_INDEX, Integer.valueOf(values[index++]));

        String imei = values[index++];
        position.set(Position.KEY_TYPE, values[index++]);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = values[index++];
        String time = values[index++];

        position.setTime(dateFormat.parse(zeros(date, 6 - date.trim().length())
                        + zeros(time, 6 - time.trim().length())));

        position.setLatitude(convert2decimal(Long.valueOf(values[index++])));
        position.setLongitude(convert2decimal(Long.valueOf(values[index++])));

        position.setSpeed(UnitsConverter.knotsFromKph(Integer.valueOf(values[index++]) / 100));
        position.setCourse(Integer.valueOf(values[index++]) / 100);

        position.set(Position.KEY_ODOMETER, Integer.valueOf(values[index++]));

        String input = decimalToBinary(Integer.valueOf(values[index++]));
        String output = decimalToBinary(Integer.valueOf(values[index++]));

        /** inputs */
        String in = new StringBuilder(zeros(input, 8 - input.length())).reverse().toString();

        /** outputs */
        String out = new StringBuilder(zeros(output, 8 - output.length())).reverse().toString();

        if (in.substring(0, 1).equals("1")) {
            position.set(Position.KEY_ALARM, Position.ALARM_SOS);
        }

        position.set(Position.PREFIX_IN + 1, in.substring(1, 2).equals("1"));
        position.set(Position.PREFIX_IN + 2, in.substring(2, 3).equals("1"));
        position.set(Position.PREFIX_IN + 3, in.substring(3, 4).equals("1"));
        position.set(Position.KEY_IGNITION, in.substring(4, 5).equals("1"));

        if (in.substring(7, 8).equals("1")) {
            position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
        }

        position.setValid(out.substring(0, 1).equals("1"));

        if (out.substring(1, 2).equals("1")) {
            position.set(Position.KEY_ALARM, Position.ALARM_JAMMING);
        }

        position.set(Position.PREFIX_OUT + 1, out.substring(2, 3).equals("1"));
        position.set(Position.PREFIX_OUT + 2, out.substring(3, 4).equals("1"));
        position.set(Position.PREFIX_OUT + 3, out.substring(4, 5).equals("1"));

        position.set(Position.KEY_POWER, Integer.valueOf(values[index++]) / 100);

        position.set(Position.KEY_BATTERY_LEVEL, Double.parseDouble(values[index++]));

        position.set(Position.KEY_RSSI, Integer.valueOf(values[index++]));

        if (values.length == 22) {
            String driverUniqueId = values[index++];
            if (!driverUniqueId.isEmpty()) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, driverUniqueId);
            }
        }

        String status = decimalToBinary(Integer.parseInt(values[index++]));
        String st = new StringBuilder(zeros(status, 8 - status.length())).reverse().toString();

        position.set(Position.ALARM_CORNERING, st.substring(0, 1).equals("1"));
        position.set(Position.ALARM_GEOFENCE_ENTER, st.substring(1, 2).equals("1"));
        position.set(Position.ALARM_FALL_DOWN, st.substring(3, 4).equals("1"));
        position.set(Position.ALARM_OVERSPEED, st.substring(4, 5).equals("1"));
        position.set("connectedPrimaryServer", st.substring(5, 6).equals("1"));
        position.set("connectedSecundaryServer", st.substring(6, 7).equals("1"));

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (sentence.contains(":")) {

            Log.info(sentence);

            String[] values = sentence.substring(1).split(":");

            String imei = values[1];

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

            if (deviceSession != null) {
                sendResponse(channel, "@");
            }

        } else {

            Position position = decodePosition(channel, remoteAddress,
                                               sentence.substring(sentence.indexOf('[', 1) + 1));

            if (position != null) {
                sendResponse(channel, "@");
                return position;
            }
        }

        return null;

    }

}
