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
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.math.BigInteger;

public class JmakProtocolDecoder extends BaseProtocolDecoder {

    public JmakProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static String hexToBinString(String hex) {
        BigInteger value = new BigInteger(hex, 16);
        String bin = value.toString(2);
        int numBits = hex.length() * 4;
        if (bin.length() < numBits) {
            bin = String.format("%" + numBits + "s", bin).replace(' ', '0');
        }
        return bin;
    }

    private static Map<Integer, String> parseFlaggedData(String hex, String[] parts) {
        String bin = hexToBinString(hex);
        Map<Integer, String> map = new HashMap<>();
        int index = 1;
        for (int i = 0; i < bin.length(); i++) {
            if (bin.charAt(bin.length() - 1 - i) == '1' && index < parts.length) {
                map.put(i + 1, parts[index++]);
            }
        }
        return map;
    }

    private static Map<String, Integer> parseOnOffCan(String decimal) {
        int value = Integer.parseInt(decimal);
        String bin6 = new StringBuilder(
                String.format("%6s", Integer.toBinaryString(value)).replace(' ', '0')
        ).reverse().toString();
        Map<String, Integer> info = new HashMap<>();
        info.put("canParkingBrake", bin6.charAt(0) == '1' ? 1 : 0);
        info.put("canBrake",         bin6.charAt(1) == '1' ? 1 : 0);
        info.put("canClutch",        bin6.charAt(2) == '1' ? 1 : 0);
        info.put("canAirConditioning", bin6.charAt(3) == '1' ? 1 : 0);
        info.put("canSeatBelt",        bin6.charAt(4) == '1' ? 1 : 0);
        return info;
    }

    private static Map<String, Integer> parseEstadoIo(String decimal) {
        int value = Integer.parseInt(decimal);
        String bin4 = new StringBuilder(
                String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0')
        ).reverse().toString();
        Map<String, Integer> info = new HashMap<>();
        info.put("input1", bin4.charAt(0) == '1' ? 1 : 0);
        info.put("input2", bin4.charAt(1) == '1' ? 1 : 0);
        info.put("output1", bin4.charAt(2) == '1' ? 1 : 0);
        info.put("output2", bin4.charAt(3) == '1' ? 1 : 0);
        return info;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();
        if (sentence.startsWith("{") || sentence.startsWith("^")) {
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(
                        Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII),
                        remoteAddress
                ));
            }
            return null;
        }

        if (!sentence.startsWith("~") || !sentence.endsWith("$")) {
            return null;
        }
        sentence = sentence.substring(1, sentence.length() - 1);

        String[] parts = sentence.split("\\|", 2);
        String[] standard = parts[0].split(";");
        DeviceSession session = getDeviceSession(channel, remoteAddress, standard[2]);
        if (session == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(session.getDeviceId());

        Map<Integer, String> data = parseFlaggedData(standard[0], standard);
        int eventId = 0;
        int eventStatus = 0;
        String eventName = null;
        for (Map.Entry<Integer, String> entry : data.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            switch (key) {
                case 1:
                    position.set("serialNumber", value);
                    break;
                case 2:
                    position.set("imei", value);
                    break;
                case 3:
                    position.set(Position.PREFIX_COUNT, Long.parseLong(value));
                    break;
                case 4:
                    if(!value.equals("NULL"))
                        position.set("nickname", value);
                    break;
                case 5:
                    position.setTime(new Date(Long.parseLong(value)));
                    break;
                case 6:
                    position.setLatitude(Double.parseDouble(value));
                    break;
                case 7:
                    position.setLongitude(Double.parseDouble(value));
                    break;
                case 8:
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case 10:
                    double speedKnots = Double.parseDouble(value) * 0.5399568;
                    position.setSpeed(speedKnots);
                    break;
                case 11:
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(value));
                    break;
                case 12:
                    position.set(Position.KEY_RSSI, Integer.parseInt(value));
                    break;
                case 13:
                    position.set(Position.KEY_HDOP, Double.parseDouble(value));
                    break;
                case 14:
                    position.setCourse(Double.parseDouble(value));
                    break;
                case 15:
                    position.set(Position.KEY_IGNITION, Integer.parseInt(value) == 1);
                    break;
                case 16:
                    position.set("backup", Integer.parseInt(value) == 1);
                    break;
                case 17:
                    position.set("hourmeter", Double.parseDouble(value));
                    break;
                case 18:
                    position.set(Position.KEY_ODOMETER, Double.parseDouble(value) * 1000);
                    break;
                case 19:
                    eventId = Integer.parseInt(value);
                    position.set(Position.KEY_EVENT, eventId);
                    break;
                case 20:
                    eventStatus = Integer.parseInt(value);
                    position.set("eventStatus", eventStatus);
                    break;
                case 21:
                    eventName = value;
                    position.set("eventName", value);
                    break;
                case 22:
                    position.set(Position.KEY_VIN, Integer.parseInt(value));
                    break;
                case 23:
                    position.set(Position.KEY_BATTERY, Integer.parseInt(value));
                    break;
                case 24:
                    position.set(Position.KEY_OPERATOR, value);
                    break;
                case 25:
                    position.set("tec", value);
                    break;
                case 26:
                    long tsSend = Long.parseLong(value);
                    position.set("tsSend", tsSend);
                    break;
                case 27:
                    position.setValid(Integer.parseInt(value) >= 1);
                    break;
                case 28:
                    parseEstadoIo(value).forEach(position::set);
                    break;
                case 29:
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, value);
                    break;
                case 30:
                    position.set("trpSize", Integer.parseInt(value));
                    break;
                case 31:
                    position.set("trpMsg", value);
                    break;
                case 35:
                    double ain = Double.parseDouble(value);
                    position.set("ain", ain);
                    position.set("discharge", ain >= 5.8 ? 1 : 0);
                    break;
                default:
                    break;
            }
        }
        if (eventId == 126 && eventStatus == 4 && eventName != null) {
            position.set(Position.KEY_CARD, eventName);
        }

        if (parts.length > 1) {
            String[] can = parts[1].split(";");
            Map<Integer, String> canData = parseFlaggedData(can[0], can);
            for (Map.Entry<Integer, String> entry : canData.entrySet()) {
                int key = entry.getKey();
                String value = entry.getValue();
                switch (key) {
                    case 1:
                        position.set(Position.KEY_ODOMETER, Double.parseDouble(value) * 1000);
                        break;
                    case 2:
                        position.set("hourmeter", Double.parseDouble(value));
                        break;
                    case 3:
                        position.set("canSpeed", Double.parseDouble(value));
                        break;
                    case 4:
                        position.set("canRpm", Double.parseDouble(value));
                        break;
                    case 5:
                        parseOnOffCan(value).forEach(position::set);
                        break;
                    case 7:
                        position.set("canPedalPressure", Double.parseDouble(value));
                        break;
                    case 9:
                        position.set("canFuelLevel", Double.parseDouble(value));
                        break;
                    case 11:
                        position.set("canAutonomy", Double.parseDouble(value));
                        break;
                    case 13:
                        position.set("canFuelConsumption", Double.parseDouble(value));
                        break;
                    case 14:
                        position.set("canFuelUsed", Double.parseDouble(value));
                        break;
                    case 15:
                        position.set("canOilTemperature", Double.parseDouble(value));
                        break;
                    default:
                        break;
                }
            }
        }

        position.setNetwork(new org.traccar.model.Network());
        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage(
                    Unpooled.copiedBuffer("ACK", StandardCharsets.US_ASCII), remoteAddress));
        }
        return position;
    }
}