/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JmakProtocolDecoder extends BaseProtocolDecoder {

    public JmakProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.startsWith("{") || sentence.startsWith("^")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII), remoteAddress));
            }
            return null;
        }

        if (!sentence.startsWith("~") || !sentence.endsWith("$")) {
            return null;
        }

        String[] parts = sentence.substring(1, sentence.length() - 1).split("\\|");

        String[] values = parts[0].split(";");
        DeviceSession session = getDeviceSession(channel, remoteAddress, values[2]);
        if (session == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(session.getDeviceId());

        decodeEvent(position, values);

        if (parts.length >= 2) {
            decodeCan(position, parts[1].split(";"));
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII), remoteAddress));
        }

        return position;
    }

    private void decodeEvent(Position position, String[] values) {
        int index = 0;
        long mask = Long.parseLong(values[index++], 16);

        if (BitUtil.check(mask, 0)) {
            index += 1; // serial number
        }

        if (BitUtil.check(mask, 1)) {
            index += 1; // imei
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.PREFIX_COUNT, Long.parseLong(values[index++]));
        }

        if (BitUtil.check(mask, 3)) {
            if (!values[index++].equals("NULL")) {
                position.set("nickname", values[index - 1]);
            }
        }

        if (BitUtil.check(mask, 4)) {
            position.setTime(new Date(Long.parseLong(values[index++])));
        }

        if (BitUtil.check(mask, 5)) {
            position.setLatitude(Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 6)) {
            position.setLongitude(Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 7)) {
            position.setAltitude(Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 8)) {
            index += 1;
        }

        if (BitUtil.check(mask, 9)) {
            position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(values[index++])));
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_SATELLITES, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 11)) {
            position.set(Position.KEY_RSSI, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_HDOP, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 13)) {
            position.setCourse(Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 14)) {
            position.set(Position.KEY_IGNITION, Integer.parseInt(values[index++]) == 1);
        }

        if (BitUtil.check(mask, 15)) {
            position.set("backup", Integer.parseInt(values[index++]) == 1);
        }

        if (BitUtil.check(mask, 16)) {
            position.set(Position.KEY_HOURS, Double.parseDouble(values[index++]) * 3600000);
        }

        if (BitUtil.check(mask, 17)) {
            position.set(Position.KEY_ODOMETER, Double.parseDouble(values[index++]) * 1000);
        }

        int eventId;
        if (BitUtil.check(mask, 18)) {
            eventId = Integer.parseInt(values[index++]);
            position.set(Position.KEY_EVENT, eventId);
        } else {
            eventId = 0;
        }

        int eventStatus;
        if (BitUtil.check(mask, 19)) {
            eventStatus = Integer.parseInt(values[index++]);
            position.set("eventStatus", eventStatus);
        } else {
            eventStatus = 0;
        }

        String eventName;
        if (BitUtil.check(mask, 20)) {
            eventName = values[index++];
            position.set("eventName", eventName);
        } else {
            eventName = null;
        }

        if (eventId == 126 && eventStatus == 4 && eventName != null) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, eventName);
        }

        if (BitUtil.check(mask, 21)) {
            position.set(Position.KEY_VIN, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 22)) {
            position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 23)) {
            position.set(Position.KEY_OPERATOR, values[index++]);
        }

        if (BitUtil.check(mask, 24)) {
            index += 1; // cellular technology
        }

        if (BitUtil.check(mask, 25)) {
            position.setDeviceTime(new Date(Long.parseLong(values[index++])));
        }

        if (BitUtil.check(mask, 26)) {
            position.setValid(Integer.parseInt(values[index++]) >= 1);
        }

        if (BitUtil.check(mask, 27)) {
            int io = Integer.parseInt(values[index++]);
            position.set(Position.PREFIX_IN + 1, BitUtil.check(io, 0));
            position.set(Position.PREFIX_IN + 2, BitUtil.check(io, 1));
            position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 2));
            position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 3));
        }

        if (BitUtil.check(mask, 28)) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, values[index++]);
        }

        if (BitUtil.check(mask, 29)) {
            index += 1; // transparent message length
        }

        if (BitUtil.check(mask, 30)) {
            position.set("message", values[index++]);
        }

        if (BitUtil.check(mask, 31)) {
            index += 1;
        }

        if (BitUtil.check(mask, 32)) {
            index += 1;
        }

        if (BitUtil.check(mask, 33)) {
            index += 1;
        }

        if (BitUtil.check(mask, 34)) {
            index += 1;
        }
    }

    private void decodeCan(Position position, String[] values) {
        int index = 0;
        long mask = Long.parseLong(values[index++], 16);

        if (BitUtil.check(mask, 0)) {
            position.set(Position.KEY_OBD_ODOMETER, Double.parseDouble(values[index++]) * 1000);
        }

        if (BitUtil.check(mask, 1)) {
            position.set(Position.KEY_HOURS, Double.parseDouble(values[index++]) * 3600000);
        }

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_OBD_SPEED, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 3)) {
            position.set(Position.KEY_RPM, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 4)) {
            position.set("canStatus", Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 5)) {
            index += 1;
        }

        if (BitUtil.check(mask, 6)) {
            position.set(Position.KEY_THROTTLE, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 7)) {
            index += 1;
        }

        if (BitUtil.check(mask, 8)) {
            position.set(Position.KEY_FUEL, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 9)) {
            index += 1;
        }

        if (BitUtil.check(mask, 10)) {
            position.set("autonomy", Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 11)) {
            index += 1;
        }

        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_FUEL_CONSUMPTION, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 13)) {
            position.set(Position.KEY_FUEL_USED, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 14)) {
            position.set("oilTemperature", Double.parseDouble(values[index++]));
        }
    }

}
