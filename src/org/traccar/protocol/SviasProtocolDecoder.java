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

    public static final String MSG_KEEPALIVE = "@";

    public SviasProtocolDecoder(SviasProtocol protocol) {
        super(protocol);
    }

    private double convertCoordinates(long v) {
        return Double.valueOf(((float) ((((float) v / 1.0E7F)
                - ((int) (v / 10000000L))) * 1.6666666666666667D)) + ((int) (v / 10000000L)));
    }

    private String toBin(String v) {
        return Integer.toString(Integer.parseInt(v), 2);
    }

    private Position decodePosition(Channel channel, SocketAddress remoteAddress, String substring)
            throws ParseException {

        String[] values = substring.split(",");

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[3]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, Integer.valueOf(values[2]));

        position.set(Position.KEY_TYPE, values[4]);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = String.format("%06d", Integer.parseInt(values[5]));
        String time = String.format("%06d", Integer.parseInt(values[6]));

        position.setTime(dateFormat.parse(date + time));

        position.setLatitude(convertCoordinates(Long.valueOf(values[7])));
        position.setLongitude(convertCoordinates(Long.valueOf(values[8])));

        position.setSpeed(UnitsConverter.knotsFromKph(Integer.valueOf(values[9]) / 100));
        position.setCourse(Integer.valueOf(values[10]) / 100);

        position.set(Position.KEY_ODOMETER, Integer.valueOf(values[11]));

        String input = new StringBuilder(String.format("%08d",
                Integer.parseInt(toBin(values[12])))).reverse().toString();

        String output = new StringBuilder(String.format("%08d",
                Integer.parseInt(toBin(values[13])))).reverse().toString();

        position.set(Position.KEY_ALARM, (input.substring(0, 1).equals("1") ? Position.ALARM_SOS : null));

        position.set(Position.KEY_IGNITION, input.substring(4, 5).equals("1"));

        position.setValid(output.substring(0, 1).equals("1"));

        return position;

    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        String sentence = (String) msg;

        if (!sentence.contains(":")) {

            Position position = decodePosition(channel, remoteAddress, sentence.substring(1));

            if (position != null) {
                return position;
            }
        }

        if (channel != null) {
            channel.write(MSG_KEEPALIVE);
        }

        return null;

    }

}
