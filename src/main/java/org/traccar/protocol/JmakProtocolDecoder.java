/*
 * Copyright 2025 - 2026 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
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

    private void sendResponse(Channel channel, SocketAddress remoteAddress) {
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII), remoteAddress));
        }
    }

    private String decodeAlarm(int event, int status) {
        return switch (event) {
            case 111, 133 -> status > 0 ? Position.ALARM_OVERSPEED : null;
            case 112 -> status > 0 ? Position.ALARM_HIGH_RPM : null;
            case 115 -> Position.ALARM_BRAKING;
            case 116 -> Position.ALARM_ACCELERATION;
            case 117 -> Position.ALARM_CORNERING;
            case 120 -> status > 0 ? Position.ALARM_GEOFENCE_ENTER : Position.ALARM_GEOFENCE_EXIT;
            case 124 -> status > 0 ? Position.ALARM_POWER_CUT : Position.ALARM_POWER_RESTORED;
            case 125 -> Position.ALARM_LOW_BATTERY;
            case 127 -> Position.ALARM_MOVEMENT;
            case 130 -> status > 1 ? Position.ALARM_JAMMING : null;
            default -> null;
        };
    }

    private void decodeEvent(Position position, int event, int status, String name) {
        position.set(Position.KEY_EVENT, event);
        position.set("eventStatus", status);
        position.set("eventName", name);
        position.set(Position.KEY_ALARM, decodeAlarm(event, status));
        if (event == 126 && name != null) {
            position.set(Position.KEY_CARD, name);
            if (status != 2 && status != 5) { // turn off or invalid tag
                position.set(Position.KEY_DRIVER_UNIQUE_ID, name);
            }
        }
    }

    private void decodeIo(Position position, int io) {
        position.set(Position.PREFIX_IN + 1, BitUtil.check(io, 0));
        position.set(Position.PREFIX_IN + 2, BitUtil.check(io, 1));
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(io, 2));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(io, 3));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(io, 4));
        position.set(Position.PREFIX_IN + 4, BitUtil.check(io, 5));
        position.set(Position.PREFIX_OUT + 3, BitUtil.check(io, 6));
    }

    private void decodeDriver(Position position, String tag) {
        if (!tag.equals("0")) {
            position.set(Position.KEY_DRIVER_UNIQUE_ID, tag);
        }
    }

    private String decodeVersion(ByteBuf buf) {
        String version = String.format("%d.%d.%d.%d",
                buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        String build = buf.readCharSequence(4, StandardCharsets.US_ASCII).toString().trim();
        return build.isEmpty() ? version : version + "-" + build;
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

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        if (sentence.startsWith("{") || sentence.startsWith("^")) {
            sendResponse(channel, remoteAddress);
            return null;
        }

        if (!sentence.startsWith("~") || !sentence.endsWith("$")) {
            return null;
        }

        return decodeText(channel, remoteAddress, sentence);
    }

    private Object decodeText(Channel channel, SocketAddress remoteAddress, String sentence) {

        String[] parts = sentence.substring(1, sentence.length() - 1).split("\\|");
        String[] values = parts[0].split(";");

        int index = 0;
        long mask = Long.parseLong(values[index++], 16);

        if (BitUtil.check(mask, 0)) {
            index += 1; // serial number
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_INDEX, Long.parseLong(values[index++]));
        }

        if (BitUtil.check(mask, 3)) {
            String nickname = values[index++];
            if (!nickname.equals("NULL")) {
                position.set("nickname", nickname);
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
            index += 1; // flags
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
            position.set(Position.KEY_IGNITION, Integer.parseInt(values[index++]) > 0);
        }

        if (BitUtil.check(mask, 15)) {
            position.set(Position.KEY_ARCHIVE, Integer.parseInt(values[index++]) > 0);
        }

        if (BitUtil.check(mask, 16)) {
            position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(Double.parseDouble(values[index++])));
        }

        if (BitUtil.check(mask, 17)) {
            position.set(Position.KEY_ODOMETER, Double.parseDouble(values[index++]) * 1000);
        }

        int event = BitUtil.check(mask, 18) ? Integer.parseInt(values[index++]) : 0;
        int status = BitUtil.check(mask, 19) ? Integer.parseInt(values[index++]) : 0;
        String name = BitUtil.check(mask, 20) ? values[index++] : null;
        if (event > 0) {
            decodeEvent(position, event, status, name);
        }

        if (BitUtil.check(mask, 21)) {
            position.set(Position.KEY_POWER, Integer.parseInt(values[index++]) / 1000.0);
        }

        if (BitUtil.check(mask, 22)) {
            position.set(Position.KEY_BATTERY, Integer.parseInt(values[index++]) / 1000.0);
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
            position.setValid(Integer.parseInt(values[index++]) > 0);
        }

        if (BitUtil.check(mask, 27)) {
            decodeIo(position, Integer.parseInt(values[index++]));
        }

        if (BitUtil.check(mask, 28)) {
            decodeDriver(position, values[index++]);
        }

        if (BitUtil.check(mask, 29)) {
            index += 1; // serial data length
        }

        if (BitUtil.check(mask, 30)) {
            String serial = values[index++];
            if (!serial.equals("NULL")) {
                position.set("serial", serial);
            }
        }

        if (BitUtil.check(mask, 31)) {
            index += 1; // custom event id
        }

        if (BitUtil.check(mask, 32)) {
            index += 1; // custom event start
        }

        if (BitUtil.check(mask, 33)) {
            index += 1; // custom event end
        }

        if (BitUtil.check(mask, 34)) {
            position.set(Position.PREFIX_ADC + 1, Double.parseDouble(values[index++]));
        }

        if (BitUtil.check(mask, 35)) {
            position.set(Position.KEY_VERSION_FW, decodeVersion(
                    Unpooled.wrappedBuffer(DataConverter.parseHex(values[index++]))));
        }

        if (BitUtil.check(mask, 36)) {
            position.set("model", values[index++]);
        }

        if (BitUtil.check(mask, 37)) {
            position.set("tripId", Long.parseLong(values[index++]));
        }

        if (parts.length > 1) {
            decodeCan(position, parts[1].split(";"));
        }

        sendResponse(channel, remoteAddress);

        return position;
    }

}
