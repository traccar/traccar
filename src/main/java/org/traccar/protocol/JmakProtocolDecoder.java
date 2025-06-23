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

    private static final Map<Integer, String> STANDARD_DATA_MAP = new HashMap<>();
    private static final Map<Integer, String> CAN_DATA_MAP = new HashMap<>();

    static {
        STANDARD_DATA_MAP.put(1, "J_SERIAL_NUMBER");                      // serial number
        STANDARD_DATA_MAP.put(2, "J_IMEI");                               // IMEI
        STANDARD_DATA_MAP.put(3, "J_COUNT");                              // count
        STANDARD_DATA_MAP.put(4, "J_NICKNAME");                           // nickname
        STANDARD_DATA_MAP.put(5, "J_TIMESTAMP");                          // timestamp
        STANDARD_DATA_MAP.put(6, "J_LATITUDE");                           // latitude
        STANDARD_DATA_MAP.put(7, "J_LONGITUDE");                          // longitude
        STANDARD_DATA_MAP.put(8, "J_ALTITUDE");                           // altitude
        STANDARD_DATA_MAP.put(9, "J_SIGNAL_LAT_LONG_ALT_REAL_TIME");      // latitude, longitude, altitude signal
        STANDARD_DATA_MAP.put(10, "J_SPEED");                             // speed
        STANDARD_DATA_MAP.put(11, "J_NUM_SAT");                           // number of satellites
        STANDARD_DATA_MAP.put(12, "J_RSSI");                              // rssi
        STANDARD_DATA_MAP.put(13, "J_HDOP");                              // hdop
        STANDARD_DATA_MAP.put(14, "J_AZIMUTE");                           // azimuth
        STANDARD_DATA_MAP.put(15, "J_IVE");                               // vehicle ignition
        STANDARD_DATA_MAP.put(16, "J_BACKUP");                            // backup
        STANDARD_DATA_MAP.put(17, "J_HOURMETER");                         // hourmeter
        STANDARD_DATA_MAP.put(18, "J_ODOMETER");                          // odometer
        STANDARD_DATA_MAP.put(19, "J_EVENT_ID");                          // event id
        STANDARD_DATA_MAP.put(20, "J_EVENT_STATUS");                      // event status
        STANDARD_DATA_MAP.put(21, "J_EVENT_NAME");                        // event name
        STANDARD_DATA_MAP.put(22, "J_VIN");                               // VIN supply voltage
        STANDARD_DATA_MAP.put(23, "J_BAT");                               // Internal battery voltage
        STANDARD_DATA_MAP.put(24, "J_OPE");                               // Operator
        STANDARD_DATA_MAP.put(25, "J_TEC");                               // Cellular technology
        STANDARD_DATA_MAP.put(26, "J_TIMESTAMP_SEND");                    // Timestamp send
        STANDARD_DATA_MAP.put(27, "J_GNSS_FIX");                          // GNSS fix
        STANDARD_DATA_MAP.put(28, "J_IOS");                               // I/O status
        STANDARD_DATA_MAP.put(29, "J_TAG");                               // Tag
        STANDARD_DATA_MAP.put(30, "J_TRP_SIZE");                          // Transparent msg size
        STANDARD_DATA_MAP.put(31, "J_TRP_MSG");                           // Transparent msg
        STANDARD_DATA_MAP.put(35, "J_AIN");                               // Analog input (mA)

        // CAN data fields
        CAN_DATA_MAP.put(1, "J_CAN_ODOMETER");                    // odometer
        CAN_DATA_MAP.put(2, "J_CAN_HOURMETER");                   // hourmeter
        CAN_DATA_MAP.put(3, "J_CAN_SPEED");                       // speed
        CAN_DATA_MAP.put(4, "J_CAN_RPM");                         // rpm
        CAN_DATA_MAP.put(5, "J_CAN_PARKING_BRAKE");               // parking brake
        CAN_DATA_MAP.put(6, "J_CAN_BRAKE");                       // brake
        CAN_DATA_MAP.put(7, "J_CAN_PEDAL_PRESSURE");              // pressão do pedal
        CAN_DATA_MAP.put(8, "J_CAN_UNIT_FUEL_LEVEL");             // fuel level unit
        CAN_DATA_MAP.put(9, "J_CAN_FUEL_LEVEL");                  // fuel level
        CAN_DATA_MAP.put(10, "J_CAN_CLUTCH");                     // clutch
        CAN_DATA_MAP.put(11, "J_CAN_AUTONOMY");                   // autonomy
        CAN_DATA_MAP.put(12, "J_CAN_UNIT_FUEL_CONSUMPTION");      // fuel consumption unit
        CAN_DATA_MAP.put(13, "J_CAN_FUEL_CONSUMPTION");           // fuel consumption
        CAN_DATA_MAP.put(14, "J_CAN_FUEL_USED");                  // fuel used
        CAN_DATA_MAP.put(15, "J_CAN_OIL_TEMPERATURE");            // oil temperature
        CAN_DATA_MAP.put(16, "J_CAN_AIR_CONDITIONING");           // air conditioning
        CAN_DATA_MAP.put(17, "J_CAN_SEAT_BELT");                  // seat belt
    }

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
                String.format("%6s", Integer.toBinaryString(value))
                        .replace(' ', '0')
        ).reverse().toString();
        Map<String, Integer> info = new HashMap<>();
        info.put("J_CAN_PARKING_BRAKE_ID", bin6.charAt(0) == '1' ? 1 : 0);
        info.put("J_CAN_BRAKE_ID",         bin6.charAt(1) == '1' ? 1 : 0);
        info.put("J_CAN_CLUTCH_ID",        bin6.charAt(2) == '1' ? 1 : 0);
        info.put("J_CAN_AIR_CONDITIONING", bin6.charAt(3) == '1' ? 1 : 0);
        info.put("J_CAN_SEAT_BELT",        bin6.charAt(4) == '1' ? 1 : 0);
        // EN: If you eventually want to include the cleaner, just uncomment it:
        // PT: Se eventualmente quiser incluir o limpador, basta descomentar:
        // info.put("J_CAN_WIPER_ID",      bin6.charAt(5) == '1' ? 1 : 0); // Limpador de para-brisa
        return info;
    }

    private static Map<String, Integer> parseEstadoIo(String decimal) {
        int value = Integer.parseInt(decimal);
        String bin4 = new StringBuilder(
                String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0')
        ).reverse().toString();
        Map<String, Integer> info = new HashMap<>();
        info.put("J_INPUT_1", bin4.charAt(0) == '1' ? 1 : 0);
        info.put("J_INPUT_2", bin4.charAt(1) == '1' ? 1 : 0);
        info.put("J_OUTPUT_1", bin4.charAt(2) == '1' ? 1 : 0);
        info.put("J_OUTPUT_2", bin4.charAt(3) == '1' ? 1 : 0);
        return info;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();
        // EN: It only returns ACK, for now the JSON and ZIP protocols are not implemented!
        // PT: Apenas retorna ACK, por enquanto não está implementado o protocolo JSON e ZIP!
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
        String[] standardParts = parts[0].split(";");
        if (standardParts.length < 3) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, standardParts[2]
        );
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        int event_id = 0;
        int event_status = 0;
        String event_name = "";
        Map<Integer, String> stdMap = parseFlaggedData(standardParts[0], standardParts);
        for (Map.Entry<Integer, String> e : stdMap.entrySet()) {
            int key = e.getKey();
            String value = e.getValue();
            String name = STANDARD_DATA_MAP.get(key);
            switch (key) {
                case 3:
                    position.set(Position.PREFIX_COUNT, Long.parseLong(value));
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
                case 18:
                    position.set(Position.KEY_ODOMETER, Double.parseDouble(value) * 1000);
                    break;
                case 19:
                    event_id = Integer.parseInt(value);
                    position.set(Position.KEY_EVENT, event_id);
                    position.set(name, event_id);
                    break;
                case 20:
                    event_status = Integer.parseInt(value);
                    position.set(name, event_status);
                    break;
                case 21:
                    event_name = value;
                    position.set(name, value);
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
                case 27:
                    position.setValid(Integer.parseInt(value) >= 1);
                    break;
                case 28:
                    for (Map.Entry<String, Integer> sub : parseEstadoIo(value).entrySet()) {
                        position.set(sub.getKey(), sub.getValue());
                    }
                    break;
                case 29:
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, value);
                    break;
                case 35:
                    double ain = Double.parseDouble(value);
                    position.set(name, ain);
                    position.set("J_DISCHARGE", ain >= 5.8 ? 1 : 0);
                    break;
                default:
                    if (name != null) {
                        position.set(name, value);
                    }
                    break;
            }
        }

        if(event_id == 126 && event_status == 4)
        {
            position.set(Position.KEY_CARD, event_name);
        }

        if (parts.length > 1) {
            String[] canParts = parts[1].split(";");
            Map<Integer, String> canMap = parseFlaggedData(canParts[0], canParts);
            for (Map.Entry<Integer, String> e : canMap.entrySet()) {
                int key = e.getKey();
                String value = e.getValue();
                String name = CAN_DATA_MAP.get(key);
                switch (key) {
                    case 1:
                        position.set(Position.KEY_ODOMETER, Double.parseDouble(value) * 1000);
                        break;
                    case 2, 3, 4, 9, 13, 14, 15:
                        position.set(name, Double.parseDouble(value));
                        break;
                    case 5:
                        for (Map.Entry<String, Integer> sub : parseOnOffCan(value).entrySet()) {
                            position.set(sub.getKey(), sub.getValue());
                        }
                    default:
                        if (name != null) {
                            position.set(name, value);
                        }
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
