/*
 * Copyright 2026 Drew Taylor (Drew.Taylor@fognetx.com)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;

public class MictrackMT700ProtocolDecoder extends BaseProtocolDecoder {

    public MictrackMT700ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String decodeAlarm(String event) {
        return switch (event) {
            case "SHAKE" -> Position.ALARM_VIBRATION;
            case "TOWED" -> Position.ALARM_TOW;
            case "DEF" -> Position.ALARM_TAMPERING;
            case "BLP" -> Position.ALARM_LOW_BATTERY;
            case "SOS" -> Position.ALARM_SOS;
            case "OVERSPEED" -> Position.ALARM_OVERSPEED;
            case "OS" -> Position.ALARM_GEOFENCE_EXIT;
            case "RS" -> Position.ALARM_GEOFENCE_ENTER;
            default -> null;
        };
    }

    private void parseCellTower(Position position, String data) {
        // Format: MCC,MNC,LAC,CellID (LAC and CellID are hex for CAT-M1/NB-IoT)
        String[] parts = data.split(",");
        if (parts.length < 4) {
            return;
        }
        try {
            int mcc = Integer.parseInt(parts[0].trim());
            int mnc = Integer.parseInt(parts[1].trim());
            long lac = Long.parseLong(parts[2].trim(), 16);
            long cid = Long.parseLong(parts[3].trim(), 16);
            Network network = position.getNetwork() != null ? position.getNetwork() : new Network();
            network.addCellTower(CellTower.from(mcc, mnc, (int) lac, cid));
            position.setNetwork(network);
        } catch (NumberFormatException e) {
            // ignore malformed cell data
        }
    }

    private static double parseNmea(String value) {
        // ddmm.mmmm or dddmm.mmmm → decimal degrees
        int dot = value.indexOf('.');
        double minutes = Double.parseDouble(value.substring(dot - 2));
        double degrees = Integer.parseInt(value.substring(0, dot - 2));
        return degrees + minutes / 60.0;
    }

    private Position decodeGprmc(Position position, String sentence) {
        // $GPRMC,hhmmss.ss,A/V,lat,NS,lon,EW,speed,course,ddmmyy,...
        int starIndex = sentence.indexOf('*');
        if (starIndex >= 0) {
            sentence = sentence.substring(0, starIndex);
        }

        String[] f = sentence.split(",", -1);
        if (f.length < 10) {
            return null;
        }

        String timeStr = f[1];
        String dateStr = f[9];
        if (timeStr.length() < 6 || dateStr.length() < 6) {
            return null;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(Integer.parseInt(timeStr.substring(0, 2)),
                        Integer.parseInt(timeStr.substring(2, 4)),
                        Integer.parseInt(timeStr.substring(4, 6)))
                .setDateReverse(Integer.parseInt(dateStr.substring(0, 2)),
                        Integer.parseInt(dateStr.substring(2, 4)),
                        Integer.parseInt(dateStr.substring(4, 6)));

        boolean valid = f[2].equals("A");
        position.setValid(valid);

        if (valid && !f[3].isEmpty() && !f[5].isEmpty()) {
            double lat = parseNmea(f[3]);
            double lon = parseNmea(f[5]);
            position.setLatitude(f[4].equals("S") ? -lat : lat);
            position.setLongitude(f[6].equals("W") ? -lon : lon);
            if (!f[7].isEmpty()) {
                position.setSpeed(Double.parseDouble(f[7]));
            }
            if (!f[8].isEmpty()) {
                position.setCourse(Double.parseDouble(f[8]));
            }
            position.setTime(dateBuilder.getDate());
        } else {
            getLastLocation(position, dateBuilder.getDate());
        }

        return position;
    }

    private Position decodeWifi(Position position, String sentence) {
        // $WIFI,hhmmss.ss,A/V,rssi,mac,...,ddmmyy*CS
        int starIndex = sentence.indexOf('*');
        if (starIndex >= 0) {
            sentence = sentence.substring(0, starIndex);
        }

        String[] parts = sentence.split(",");
        if (parts.length < 3) {
            return null;
        }

        // parts[0]=$WIFI, parts[1]=time, parts[2]=status
        // pairs [3..n-2]: rssi, mac  (variable count)
        // parts[n-1]=date (ddmmyy)
        String timeStr = parts[1];
        if (timeStr.length() < 6) {
            return null;
        }
        int hour = Integer.parseInt(timeStr.substring(0, 2));
        int minute = Integer.parseInt(timeStr.substring(2, 4));
        int second = Integer.parseInt(timeStr.substring(4, 6));

        String dateStr = parts[parts.length - 1];
        if (dateStr.length() < 6) {
            return null;
        }
        int day = Integer.parseInt(dateStr.substring(0, 2));
        int month = Integer.parseInt(dateStr.substring(2, 4));
        int year = 2000 + Integer.parseInt(dateStr.substring(4, 6));

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(year, month, day)
                .setTime(hour, minute, second);

        Network network = position.getNetwork() != null ? position.getNetwork() : new Network();

        // WiFi pairs occupy indices [3..parts.length-2], stepping by 2
        for (int i = 3; i + 1 < parts.length - 1; i += 2) {
            try {
                int rssi = Integer.parseInt(parts[i].trim());
                String mac = parts[i + 1].trim();
                if (mac.length() == 12) {
                    String formatted = mac.replaceAll("(?<=\\G.{2})(?=.)", ":").toLowerCase();
                    network.addWifiAccessPoint(WifiAccessPoint.from(formatted, rssi));
                }
            } catch (NumberFormatException e) {
                // ignore malformed entry
            }
        }

        position.setNetwork(network);
        getLastLocation(position, dateBuilder.getDate());

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String sentence = ((String) msg).trim();

        if (!sentence.startsWith("#")) {
            return null;
        }

        // Message: #IMEI#MT700#0000#EVENT#1\r\n#voltage[#cell]$GPRMC/$WIFI,...\r\n
        String[] lines = sentence.split("\r?\n");
        if (lines.length < 2) {
            return null;
        }

        // Header: #IMEI#MT700W?#0000#EVENT#1
        String[] header = lines[0].split("#");
        // [0]="", [1]=IMEI, [2]=MT700/MT700W, [3]=0000, [4]=event, [5]=1
        if (header.length < 5) {
            return null;
        }
        String imei = header[1];
        String event = header[4];

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.addAlarm(decodeAlarm(event));

        // Body: #voltage[#cellinfo]$GPRMC/WIFI,...
        String body = lines[1];
        if (!body.startsWith("#")) {
            return null;
        }
        body = body.substring(1); // strip leading #

        int dollarIndex = body.indexOf('$');
        if (dollarIndex < 0) {
            return null;
        }

        String prefix = body.substring(0, dollarIndex);  // e.g. "3815" or "3815#" or "3815#460,00,1D29,abc"
        String gpsData = body.substring(dollarIndex);

        String[] prefixParts = prefix.split("#", 2);
        try {
            int voltageRaw = Integer.parseInt(prefixParts[0].trim());
            position.set(Position.KEY_BATTERY, voltageRaw > 100 ? voltageRaw / 1000.0 : voltageRaw / 10.0);
        } catch (NumberFormatException e) {
            // ignore
        }

        if (prefixParts.length > 1 && !prefixParts[1].isEmpty()) {
            parseCellTower(position, prefixParts[1]);
        }

        if (gpsData.startsWith("$GPRMC")) {
            return decodeGprmc(position, gpsData);
        } else if (gpsData.startsWith("$WIFI")) {
            return decodeWifi(position, gpsData);
        }

        return null;
    }

}
