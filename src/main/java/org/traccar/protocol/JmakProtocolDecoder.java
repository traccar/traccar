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
import io.netty.buffer.ByteBufUtil;
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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
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

    private void decodeCanValue(Position position, int index, double value, int unit) {
        switch (index) {
            case 0 -> position.set(Position.KEY_OBD_ODOMETER, value * 1000);
            case 1 -> position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(value));
            case 2 -> position.set(Position.KEY_OBD_SPEED, value);
            case 3 -> position.set(Position.KEY_RPM, value);
            case 4 -> position.set("canStatus", (int) value);
            case 6 -> position.set(Position.KEY_THROTTLE, value);
            case 8 -> position.set(unit == 'L' ? Position.KEY_FUEL : Position.KEY_FUEL_LEVEL, value);
            case 10 -> position.set("autonomy", value);
            case 12 -> position.set(Position.KEY_FUEL_CONSUMPTION, value);
            case 13 -> position.set(Position.KEY_FUEL_USED, value);
            case 14 -> position.set("oilTemp", value);
            case 15 -> position.set("tripFuelUsed", value);
            case 16 -> position.set("engineTorque", value);
            case 17 -> position.set(Position.KEY_ENGINE_LOAD, value);
            case 18 -> position.set("fuelEconomy", value);
            case 19 -> position.set("oilPressure", value);
            case 20 -> position.set("brakePressure1", value);
            case 21 -> position.set("brakePressure2", value);
            case 22 -> position.set("pto", (int) value);
            case 23 -> position.set("gear", (int) value);
            case 24 -> position.set("engineBrake", value);
            case 25 -> position.set("transmissionOilTemp", value);
            case 26 -> position.set("transmissionOilPressure", value);
            case 27 -> position.set("boostPressure", value);
            case 28 -> position.set("intakeTemp", value);
            case 29 -> position.set("fuelTemp", value);
            case 30 -> position.set("adBlueLevel", value);
            case 31 -> position.set("adBlueTemp", value);
            case 32 -> position.set("scrPressure", value);
            case 33 -> position.set("dpfPressure", value);
            case 34 -> position.set("dpfRegeneration", (int) value);
            case 35 -> position.set("ambientTemperature", value);
            case 36 -> position.set("batteryVoltage", value);
            case 37 -> position.set("wiper", (int) value);
            case 38 -> position.set("instantFuelConsumption", value);
        }
    }

    private void decodeCan(Position position, String[] values) {
        int index = 0;
        long mask = Long.parseLong(values[index++], 16);
        int fuelUnit = 0;
        for (int i = 0; i < 39; i++) {
            if (BitUtil.check(mask, i)) {
                String value = values[index++];
                if (i == 7) {
                    fuelUnit = value.charAt(0);
                } else if (i != 11) { // consumption unit
                    decodeCanValue(position, i, Double.parseDouble(value), fuelUnit);
                }
            }
        }
    }

    private void decodeCan(Position position, ByteBuf buf) {
        long mask = buf.readLong();
        int fuelUnit = 0;
        for (int i = 0; i < 39; i++) {
            if (BitUtil.check(mask, i)) {
                double value = switch (i) {
                    case 0, 1, 13 -> ((long) buf.readUnsignedByte() << 32 | buf.readUnsignedInt()) / 100.0;
                    case 2, 3, 10 -> buf.readUnsignedMedium() / 100.0;
                    case 12, 15, 18, 38 -> buf.readUnsignedShort() / 100.0;
                    case 4 -> buf.readUnsignedShort();
                    case 5, 9 -> 0;
                    case 14, 28, 29, 31 -> buf.readUnsignedByte() - 40;
                    case 19, 20, 21 -> buf.readUnsignedByte() * 4.0;
                    case 23, 24 -> buf.readUnsignedByte() - 125;
                    case 25, 35 -> buf.readUnsignedShort() - 273;
                    case 26 -> buf.readUnsignedByte() * 16.0;
                    case 27 -> buf.readUnsignedByte() * 2.0;
                    case 32 -> buf.readUnsignedByte() * 8.0;
                    case 33 -> buf.readUnsignedShort() / 10.0;
                    case 36 -> buf.readUnsignedShort() / 20.0;
                    default -> buf.readUnsignedByte();
                };
                if (i == 7) {
                    fuelUnit = (int) value;
                } else if (i != 11) { // consumption unit
                    decodeCanValue(position, i, value, fuelUnit);
                }
            }
        }
    }

    private void decodeCan(Position position, JsonObject can) {
        for (String key : can.keySet()) {
            int id = Integer.parseInt(key);
            int index = switch (id) {
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 0;
                case 4 -> 8;
                case 5 -> 14;
                case 6 -> 6;
                case 7 -> 4;
                case 9 -> 10;
                case 10 -> 12;
                case 11 -> 1;
                case 14 -> 13;
                case 40 -> 38;
                default -> id >= 16 && id <= 38 ? id - 1 : -1;
            };
            if (index < 0) {
                continue;
            }
            JsonValue value = can.get(key);
            if (value instanceof JsonString string) {
                String data = string.getString();
                int unit = Character.isLetter(data.charAt(0)) ? data.charAt(0) : 0;
                double number = Double.parseDouble(unit > 0 ? data.substring(1) : data);
                decodeCanValue(position, index, number, unit);
            } else if (value instanceof JsonNumber number) {
                decodeCanValue(position, index, number.doubleValue(), 0);
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        return switch (buf.getByte(buf.readerIndex())) {
            case '~' -> decodeText(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));
            case '^' -> decodeBinary(channel, remoteAddress, buf);
            case '{' -> decodeJson(channel, remoteAddress, buf.toString(StandardCharsets.UTF_8));
            case '*' -> decodeSummary(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));
            case '$' -> decodeResult(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));
            default -> null;
        };
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

    private Object decodeBinary(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedByte(); // header
        long mask = buf.readLong();

        if (BitUtil.check(mask, 0)) {
            buf.skipBytes(11); // serial number
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(buf.readLong()));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (BitUtil.check(mask, 2)) {
            position.set(Position.KEY_INDEX, buf.readUnsignedInt());
        }

        if (BitUtil.check(mask, 3)) {
            position.set("nickname", buf.readCharSequence(10, StandardCharsets.US_ASCII).toString().trim());
        }

        if (BitUtil.check(mask, 4)) {
            position.setTime(new Date(buf.readLong()));
        }

        double latitude = BitUtil.check(mask, 5) ? buf.readUnsignedInt() / 100000.0 : 0;
        double longitude = BitUtil.check(mask, 6) ? buf.readUnsignedInt() / 100000.0 : 0;
        double altitude = BitUtil.check(mask, 7) ? buf.readUnsignedMedium() / 100.0 : 0;

        if (BitUtil.check(mask, 8)) {
            int flags = buf.readUnsignedByte();
            latitude = BitUtil.check(flags, 0) ? -latitude : latitude;
            longitude = BitUtil.check(flags, 1) ? -longitude : longitude;
            altitude = BitUtil.check(flags, 2) ? -altitude : altitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setAltitude(altitude);

        if (BitUtil.check(mask, 9)) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedMedium() / 100.0));
        }

        if (BitUtil.check(mask, 10)) {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 11)) {
            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 12)) {
            position.set(Position.KEY_HDOP, buf.readUnsignedShort() / 100.0);
        }

        if (BitUtil.check(mask, 13)) {
            position.setCourse(buf.readUnsignedMedium() / 100.0);
        }

        if (BitUtil.check(mask, 14)) {
            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
        }

        if (BitUtil.check(mask, 15)) {
            position.set(Position.KEY_ARCHIVE, buf.readUnsignedByte() > 0);
        }

        if (BitUtil.check(mask, 16)) {
            long hours = (long) buf.readUnsignedByte() << 32 | buf.readUnsignedInt();
            position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(hours / 100.0));
        }

        if (BitUtil.check(mask, 17)) {
            long odometer = (long) buf.readUnsignedByte() << 32 | buf.readUnsignedInt();
            position.set(Position.KEY_ODOMETER, odometer * 10);
        }

        int event = BitUtil.check(mask, 18) ? buf.readUnsignedByte() : 0;
        int status = BitUtil.check(mask, 19) ? buf.readUnsignedByte() : 0;
        String name = BitUtil.check(mask, 20) ? Long.toUnsignedString(buf.readLong()) : null;
        if (event > 0) {
            decodeEvent(position, event, status, name);
        }

        if (BitUtil.check(mask, 21)) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() / 1000.0);
        }

        if (BitUtil.check(mask, 22)) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }

        if (BitUtil.check(mask, 23)) {
            position.set(Position.KEY_OPERATOR, buf.readCharSequence(4, StandardCharsets.US_ASCII).toString().trim());
        }

        if (BitUtil.check(mask, 24)) {
            buf.readUnsignedByte(); // cellular technology
        }

        if (BitUtil.check(mask, 25)) {
            position.setDeviceTime(new Date(buf.readLong()));
        }

        if (BitUtil.check(mask, 26)) {
            position.setValid(buf.readUnsignedByte() > 0);
        }

        if (BitUtil.check(mask, 27)) {
            decodeIo(position, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 28)) {
            decodeDriver(position, Long.toUnsignedString(buf.readLong()));
        }

        int length = BitUtil.check(mask, 29) ? buf.readUnsignedByte() : 0;

        if (BitUtil.check(mask, 30)) {
            position.set("serial", ByteBufUtil.hexDump(buf.readSlice(length)));
        }

        if (BitUtil.check(mask, 31)) {
            buf.readLong(); // custom event id
        }

        if (BitUtil.check(mask, 32)) {
            buf.readLong(); // custom event start
        }

        if (BitUtil.check(mask, 33)) {
            buf.readLong(); // custom event end
        }

        if (BitUtil.check(mask, 34)) {
            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() / 100.0);
        }

        if (BitUtil.check(mask, 35)) {
            position.set(Position.KEY_VERSION_FW, decodeVersion(buf));
        }

        if (BitUtil.check(mask, 36)) {
            position.set("model", buf.readCharSequence(8, StandardCharsets.US_ASCII).toString().trim());
        }

        if (BitUtil.check(mask, 37)) {
            position.set("tripId", buf.readUnsignedInt());
        }

        if (buf.readableBytes() > 1) {
            decodeCan(position, buf);
        }

        sendResponse(channel, remoteAddress);

        return position;
    }

    private Object decodeJson(Channel channel, SocketAddress remoteAddress, String sentence) {

        JsonObject root = Json.createReader(new StringReader(sentence)).readObject();

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, root.getJsonNumber("imei").toString());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, root.getJsonNumber("cnt").longValue());
        position.set("nickname", root.getString("name", null));

        position.setValid(root.getInt("fix") > 0);
        position.setTime(new Date(root.getJsonNumber("ts").longValue()));
        position.setDeviceTime(new Date(root.getJsonNumber("ts_n").longValue()));

        JsonArray coordinates = root.getJsonArray("gps");
        position.setLatitude(coordinates.getJsonNumber(0).doubleValue());
        position.setLongitude(coordinates.getJsonNumber(1).doubleValue());
        position.setAltitude(root.getJsonNumber("alt").doubleValue());
        position.setSpeed(UnitsConverter.knotsFromKph(root.getJsonNumber("speed").doubleValue()));
        position.setCourse(root.getJsonNumber("azi").doubleValue());

        position.set(Position.KEY_SATELLITES, root.getInt("nsat"));
        position.set(Position.KEY_HDOP, root.getJsonNumber("hdop").doubleValue());
        position.set(Position.KEY_RSSI, root.getInt("rssi"));
        position.set(Position.KEY_OPERATOR, root.getString("ope"));
        position.set(Position.KEY_IGNITION, root.getInt("ive") > 0);
        position.set(Position.KEY_ARCHIVE, root.getInt("bckup") > 0);
        position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(root.getJsonNumber("hourmt").doubleValue()));
        position.set(Position.KEY_ODOMETER, root.getJsonNumber("odomt").doubleValue() * 1000);
        position.set(Position.KEY_POWER, root.getInt("vin") / 1000.0);
        position.set(Position.KEY_BATTERY, root.getInt("bat") / 1000.0);

        if (root.containsKey("ios")) {
            decodeIo(position, root.getInt("ios"));
        }
        decodeDriver(position, root.getJsonNumber("tag").toString());

        JsonObject event = root.getJsonObject("evt");
        if (event.containsKey("id")) {
            decodeEvent(position, event.getInt("id"), event.getInt("st", 0), event.getString("name", null));
        }

        if (root.containsKey("anin")) {
            position.set(Position.PREFIX_ADC + 1, root.getJsonNumber("anin").doubleValue());
        }

        position.set("serial", root.getString("trp", null));

        String version = root.getString("ver", "");
        if (!version.isEmpty()) {
            position.set(Position.KEY_VERSION_FW, decodeVersion(
                    Unpooled.wrappedBuffer(DataConverter.parseHex(version))));
        }

        position.set("model", root.getString("mod", null));

        if (root.containsKey("trp_id")) {
            position.set("tripId", root.getJsonNumber("trp_id").longValue());
        }

        if (root.containsKey("can")) {
            decodeCan(position, root.getJsonObject("can"));
        }

        sendResponse(channel, remoteAddress);

        return position;
    }

    private Object decodeSummary(Channel channel, SocketAddress remoteAddress, String sentence) {

        String[] values = sentence.substring(1, sentence.length() - 1).split(";");

        int index = 0;
        long mask = Long.parseLong(values[index++], 16);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[index++]);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        index += 1; // serial number
        position.set("tripId", Long.parseLong(values[index++]));
        position.set(Position.KEY_ARCHIVE, Integer.parseInt(values[index++]) > 0);
        decodeDriver(position, values[index++]);

        position.set("tripStart", Long.parseLong(values[index++]));
        long tripEnd = Long.parseLong(values[index++]);
        position.set("tripEnd", tripEnd);

        position.setValid(true);
        position.setTime(new Date(tripEnd));
        index += 1; // start latitude
        position.setLatitude(Double.parseDouble(values[index++]));
        index += 1; // start longitude
        position.setLongitude(Double.parseDouble(values[index++]));
        index += 1; // flags

        double odometerStart = Double.parseDouble(values[index++]);
        double odometerEnd = Double.parseDouble(values[index++]);
        position.set(Position.KEY_ODOMETER, odometerEnd * 1000);
        position.set(Position.KEY_ODOMETER_TRIP, (odometerEnd - odometerStart) * 1000);

        double hoursStart = Double.parseDouble(values[index++]);
        double hoursEnd = Double.parseDouble(values[index++]);
        position.set(Position.KEY_HOURS, UnitsConverter.msFromHours(hoursEnd));
        position.set("tripHours", UnitsConverter.msFromHours(hoursEnd - hoursStart));

        if (BitUtil.check(mask, 17) && !values[index + 1].contains(":")) {
            double fuelStart = Double.parseDouble(values[index++]);
            double fuelEnd = Double.parseDouble(values[index++]);
            if (fuelEnd > 0) {
                position.set(Position.KEY_FUEL_USED, fuelEnd);
                position.set("tripFuelUsed", fuelEnd - fuelStart);
            }
            if (BitUtil.check(mask, 20)) {
                position.set("tripFuelUsed", Double.parseDouble(values[values.length - 1]));
            }
        }

        sendResponse(channel, remoteAddress);

        return position;
    }

    private Object decodeResult(Channel channel, SocketAddress remoteAddress, String sentence) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.set(Position.KEY_RESULT, sentence);

        return position;
    }

}
