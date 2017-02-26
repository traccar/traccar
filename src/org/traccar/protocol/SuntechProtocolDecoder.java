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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class SuntechProtocolDecoder extends BaseProtocolDecoder {

    private int protocolType;

    public SuntechProtocolDecoder(SuntechProtocol protocol) {
        super(protocol);

        protocolType = Context.getConfig().getInteger(getProtocolName() + ".protocolType");
    }

    public void setProtocolType(int protocolType) {
        this.protocolType = protocolType;
    }

    private Position decode9(
            Channel channel, SocketAddress remoteAddress, String[] values) throws ParseException {
        int index = 1;

        String type = values[index++];

        if (!type.equals("Location") && !type.equals("Emergency") && !type.equals("Alert")) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (type.equals("Emergency") || type.equals("Alert")) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_VERSION_FW, values[index++]);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(values[index++] + values[index++]));

        if (protocolType == 1) {
            index += 1; // cell
        }

        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Double.parseDouble(values[index++]));

        position.setValid(values[index++].equals("1"));

        if (protocolType == 1) {
            position.set(Position.KEY_ODOMETER, Integer.parseInt(values[index]));
        }

        return position;
    }

    private Position decode23(
            Channel channel, SocketAddress remoteAddress, String protocol, String[] values) throws ParseException {
        int index = 0;

        String type = values[index++].substring(5);

        if (!type.equals("STT") && !type.equals("EMG") && !type.equals("EVT") && !type.equals("ALT")) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (type.equals("EMG") || type.equals("ALT")) {
            position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (protocol.equals("ST300")) {
            index += 1; // model
        }

        position.set(Position.KEY_VERSION_FW, values[index++]);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(values[index++] + values[index++]));

        index += 1; // cell

        position.setLatitude(Double.parseDouble(values[index++]));
        position.setLongitude(Double.parseDouble(values[index++]));
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        position.setCourse(Double.parseDouble(values[index++]));

        position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));

        position.setValid(values[index++].equals("1"));

        position.set(Position.KEY_ODOMETER, Integer.parseInt(values[index++]));
        position.set(Position.KEY_POWER, Double.parseDouble(values[index++]));

        position.set(Position.PREFIX_IO + 1, values[index++]);

        index += 1; // mode

        if (type.equals("STT")) {
            position.set(Position.KEY_INDEX, Integer.parseInt(values[index++]));
        }

        if (index < values.length) {
            position.set(Position.KEY_HOURS, Integer.parseInt(values[index++]));
        }

        if (index < values.length) {
            position.set(Position.KEY_BATTERY, Double.parseDouble(values[index]));
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String[] values = ((String) msg).split(";");

        String protocol = values[0].substring(0, 5);

        if (protocol.equals("ST910")) {
            return decode9(channel, remoteAddress, values);
        } else {
            return decode23(channel, remoteAddress, protocol, values);
        }
    }

}
