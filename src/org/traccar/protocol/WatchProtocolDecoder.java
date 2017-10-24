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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Pattern;

public class WatchProtocolDecoder extends BaseProtocolDecoder {

    public WatchProtocolDecoder(WatchProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN_POSITION = new PatternBuilder()
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
            .expression("(.*)")                  // cell and wifi
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
        } else if (BitUtil.check(status, 20)) {
            return Position.ALARM_REMOVING;
        } else if (BitUtil.check(status, 21)) {
            return Position.ALARM_FALL_DOWN;
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

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(1); // header
        String manufacturer = buf.readBytes(2).toString(StandardCharsets.US_ASCII);
        buf.skipBytes(1); // delimiter

        String id = buf.readBytes(10).toString(StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        buf.skipBytes(1); // delimiter
        buf.skipBytes(4); // length
        buf.skipBytes(1); // delimiter

        buf.writerIndex(buf.writerIndex() - 1); // ignore ending

        int contentIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        if (contentIndex < 0) {
            contentIndex = buf.writerIndex();
        }

        String type = buf.readBytes(contentIndex - buf.readerIndex()).toString(StandardCharsets.US_ASCII);

        if (contentIndex < buf.writerIndex()) {
            buf.readerIndex(contentIndex + 1);
        }

        if (type.equals("LK")) {

            sendResponse(channel, manufacturer, id, "LK");

            if (buf.readable()) {
                String[] values = buf.toString(StandardCharsets.US_ASCII).split(",");
                if (values.length >= 3) {
                    Position position = new Position();
                    position.setProtocol(getProtocolName());
                    position.setDeviceId(deviceSession.getDeviceId());

                    getLastLocation(position, null);

                    position.set(Position.KEY_BATTERY_LEVEL, Integer.parseInt(values[2]));

                    return position;
                }
            }

        } else if (type.equals("UD") || type.equals("UD2") || type.equals("UD3")
                || type.equals("AL") || type.equals("WT")) {

            if (type.equals("AL")) {
                sendResponse(channel, manufacturer, id, "AL");
            }

            Parser parser = new Parser(PATTERN_POSITION, buf.toString(StandardCharsets.US_ASCII));
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

            position.set(Position.KEY_STEPS, parser.nextInt(0));

            int status = parser.nextHexInt(0);
            position.set(Position.KEY_ALARM, decodeAlarm(status));
            if (BitUtil.check(status, 4)) {
                position.set(Position.KEY_MOTION, true);
            }

            decodeTail(position, parser.next());

            return position;

        } else if (type.equals("TKQ")) {

            sendResponse(channel, manufacturer, id, "TKQ");

        } else if (type.equals("PULSE") || type.equals("heart")) {

            if (buf.readable()) {

                Position position = new Position();
                position.setProtocol(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                getLastLocation(position, new Date());

                position.setValid(false);
                String pulse = buf.toString(StandardCharsets.US_ASCII);
                position.set("pulse", pulse);
                position.set(Position.KEY_RESULT, pulse);

                return position;

            }

        } else if (type.equals("img")) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            int timeIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
            buf.readerIndex(timeIndex + 12 + 2);
            position.set(Position.KEY_IMAGE, Context.getMediaManager().writeFile(id, buf, "jpg"));

            return position;

        } else if (type.equals("TK")) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            getLastLocation(position, null);

            position.set(Position.KEY_AUDIO, Context.getMediaManager().writeFile(id, buf, "amr"));

            return position;

        }

        return null;
    }

}
