/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BitUtil;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;

public class WatchProtocolDecoder extends BaseProtocolDecoder {

    public WatchProtocolDecoder(WatchProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("[")
            .expression("(..)").text("*")        // manufacturer
            .number("(d+)").text("*")            // equipment id
            .number("xxxx").text("*")            // length
            .expression("([^,]+)")               // type
            .expression("(.*)")                  // content
            .compile();

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
            .text(",")
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .expression("([AV]),")               // validity
            .number(" *(-?d+.d+),")              // latitude
            .expression("([NS]),")
            .number(" *(-?d+.d+),")              // longitude
            .expression("([EW])?,")
            .number("(d+.d+),")                  // speed
            .number("(d+.?d*),")                 // course
            .number("(d+.?d*),")                 // altitude
            .number("(d+),")                     // satellites
            .number("(d+),")                     // rssi
            .number("(d+),")                     // battery
            .number("(d+),")                     // steps
            .number("d+,")                       // tumbles
            .number("(x+),")                     // status
            .expression("([^\\]]*)")             // cell and wifi
            .text("]")
            .compile();

    private void sendResponse(Channel channel, String manufacturer, String id, String content) {
        if (channel != null) {
            channel.write(String.format(
                    "[%s*%s*%04x*%s]", manufacturer, id, content.length(), content));
        }
    }

    private String decodeAlarm(int status) {
        if (BitUtil.check(status, 0)) {
            return Position.ALARM_LOW_BATTERY;
        } else if (BitUtil.check(status, 1)) {
            return Position.ALARM_GEOFENCE_EXIT;
        } else if (BitUtil.check(status, 2)) {
            return Position.ALARM_GEOFENCE_ENTER;
        } else if (BitUtil.check(status, 3)) {
            return Position.ALARM_OVERSPEED;
        } else if (BitUtil.check(status, 16)) {
            return Position.ALARM_SOS;
        } else if (BitUtil.check(status, 17)) {
            return Position.ALARM_LOW_BATTERY;
        } else if (BitUtil.check(status, 18)) {
            return Position.ALARM_GEOFENCE_EXIT;
        } else if (BitUtil.check(status, 19)) {
            return Position.ALARM_GEOFENCE_ENTER;
        }
        return null;
    }

    private void decodeTail(Position position, String data) {
        String[] values = data.split(",");
        int index = 0;

        Network network = new Network();

        int cellCount = Integer.parseInt(values[index++]);
        index += 1; // timing advance
        int mcc = Integer.parseInt(values[index++]);
        int mnc = Integer.parseInt(values[index++]);

        for (int i = 0; i < cellCount; i++) {
            network.addCellTower(CellTower.from(mcc, mnc,
                    Integer.parseInt(values[index++]), Integer.parseInt(values[index++]),
                    Integer.parseInt(values[index++])));
        }

        if (index < values.length && !values[index].isEmpty()) {
            int wifiCount = Integer.parseInt(values[index++]);

            for (int i = 0; i < wifiCount; i++) {
                index += 1; // wifi name
                network.addWifiAccessPoint(WifiAccessPoint.from(
                        values[index++], Integer.parseInt(values[index++])));
            }
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        String manufacturer = parser.next();
        String id = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        String type = parser.next();
        String content = parser.next();

        if (type.equals("LK")) {

            sendResponse(channel, manufacturer, id, "LK");

            if (!content.isEmpty()) {
                String[] values = content.split(",");
                if (values.length >= 4) {
                    Position position = new Position();
                    position.setProtocol(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    getLastLocation(position, null);

                    position.set(Position.KEY_BATTERY_LEVEL,
                            Integer.parseInt(values[3].substring(0, values[3].length() - 1)));

                    return position;
                }
            }

        } else if (type.equals("UD") || type.equals("UD2") || type.equals("UD3")
                || type.equals("AL") || type.equals("WT")) {

            if (type.equals("AL")) {
                sendResponse(channel, manufacturer, id, "AL");
            }

            parser = new Parser(PATTERN_POSITION, content);
            if (!parser.matches()) {
                return null;
            }

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.setTime(parser.nextDateTime(Parser.DateTimeFormat.DMY_HMS));

            position.setValid(parser.next().equals("A"));
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_HEM));
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
            position.setCourse(parser.nextDouble(0));
            position.setAltitude(parser.nextDouble(0));

            position.set(Position.KEY_SATELLITES, parser.nextInt(0));
            position.set(Position.KEY_RSSI, parser.nextInt(0));
            position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt(0));

            position.set("steps", parser.nextInt(0));

            position.set(Position.KEY_ALARM, decodeAlarm(parser.nextHexInt(0)));

            decodeTail(position, parser.next());

            return position;

        } else if (type.equals("TKQ")) {

            sendResponse(channel, manufacturer, id, "TKQ");

        } else if (type.equals("PULSE") || type.equals("heart")) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, new Date());
            position.setValid(false);
            String pulse = content.substring(1);
            position.set("pulse", pulse);
            position.set(Position.KEY_RESULT, pulse);
            return position;

        }

        return null;
    }

}
