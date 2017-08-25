/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MeitrackProtocolDecoder extends BaseProtocolDecoder {

    public MeitrackProtocolDecoder(MeitrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$").expression(".")          // flag
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("xxx,")                      // command
            .number("d+,").optional()
            .number("(d+),")                     // event
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("([AV]),")                   // validity
            .number("(d+),")                     // satellites
            .number("(d+),")                     // rssi
            .number("(d+.?d*),")                 // speed
            .number("(d+),")                     // course
            .number("(d+.?d*),")                 // hdop
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("(d+),")                     // runtime
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cid
            .number("(x+),")                     // state
            .number("(x+)?|")                    // adc1
            .number("(x+)?|")                    // adc2
            .number("(x+)?|")                    // adc3
            .number("(x+)|")                     // battery
            .number("(x+),")                     // power
            .groupBegin()
            .expression("([^,]+)?,")             // event specific
            .expression("[^,]*,")                // reserved
            .number("d*,")                       // protocol
            .number("(x{4})?")                   // fuel
            .number("(?:,(x{6}(?:|x{6})*))?")    // temperature
            .or()
            .any()
            .groupEnd()
            .text("*")
            .number("xx")
            .text("\r\n").optional()
            .compile();

    private String decodeAlarm(int event) {
        switch (event) {
            case 1:
                return Position.ALARM_SOS;
            case 17:
                return Position.ALARM_LOW_BATTERY;
            case 18:
                return Position.ALARM_LOW_POWER;
            case 19:
                return Position.ALARM_OVERSPEED;
            case 20:
                return Position.ALARM_GEOFENCE_ENTER;
            case 21:
                return Position.ALARM_GEOFENCE_EXIT;
            case 22:
                return Position.ALARM_POWER_RESTORED;
            case 23:
                return Position.ALARM_POWER_CUT;
            case 36:
                return Position.ALARM_TOW;
            default:
                return null;
        }
    }

    private Position decodeRegularMessage(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        Parser parser = new Parser(PATTERN, buf.toString(StandardCharsets.US_ASCII));
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        int event = parser.nextInt(0);
        position.set(Position.KEY_EVENT, event);
        position.set(Position.KEY_ALARM, decodeAlarm(event));

        position.setLatitude(parser.nextDouble(0));
        position.setLongitude(parser.nextDouble(0));

        position.setTime(parser.nextDateTime());

        position.setValid(parser.next().equals("A"));

        position.set(Position.KEY_SATELLITES, parser.nextInt());
        int rssi = parser.nextInt(0);

        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble(0)));
        position.setCourse(parser.nextDouble(0));

        position.set(Position.KEY_HDOP, parser.nextDouble());

        position.setAltitude(parser.nextDouble(0));

        position.set(Position.KEY_ODOMETER, parser.nextInt(0));
        position.set("runtime", parser.next());

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(0), parser.nextInt(0), parser.nextHexInt(0), parser.nextHexInt(0), rssi)));

        position.set(Position.KEY_STATUS, parser.next());

        for (int i = 1; i <= 3; i++) {
            if (parser.hasNext()) {
                position.set(Position.PREFIX_ADC + i, parser.nextHexInt(0));
            }
        }

        String deviceModel = Context.getIdentityManager().getById(deviceSession.getDeviceId()).getModel();
        if (deviceModel == null) {
            deviceModel = "";
        }
        switch (deviceModel.toUpperCase()) {
            case "MVT340":
            case "MVT380":
                position.set(Position.KEY_BATTERY, parser.nextHexInt(0) * 3.0 * 2.0 / 1024.0);
                position.set(Position.KEY_POWER, parser.nextHexInt(0) * 3.0 * 16.0 / 1024.0);
                break;
            case "MT90":
                position.set(Position.KEY_BATTERY, parser.nextHexInt(0) * 3.3 * 2.0 / 4096.0);
                position.set(Position.KEY_POWER, parser.nextHexInt(0));
                break;
            case "T1":
            case "T3":
            case "MVT100":
            case "MVT600":
            case "MVT800":
            case "TC68":
            case "TC68S":
                position.set(Position.KEY_BATTERY, parser.nextHexInt(0) * 3.3 * 2.0 / 4096.0);
                position.set(Position.KEY_POWER, parser.nextHexInt(0) * 3.3 * 16.0 / 4096.0);
                break;
            case "T311":
            case "T322X":
            case "T333":
            case "T355":
                position.set(Position.KEY_BATTERY, parser.nextHexInt(0) / 100.0);
                position.set(Position.KEY_POWER, parser.nextHexInt(0) / 100.0);
                break;
            default:
                position.set(Position.KEY_BATTERY, parser.nextHexInt(0));
                position.set(Position.KEY_POWER, parser.nextHexInt(0));
                break;
        }

        String eventData = parser.next();
        if (eventData != null && !eventData.isEmpty()) {
            switch (event) {
                case 37:
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, eventData);
                    break;
                default:
                    position.set("eventData", eventData);
                    break;
            }
        }

        if (parser.hasNext()) {
            String fuel = parser.next();
            position.set(Position.KEY_FUEL_LEVEL,
                    Integer.parseInt(fuel.substring(0, 2), 16) + Integer.parseInt(fuel.substring(2), 16) * 0.01);
        }

        if (parser.hasNext()) {
            for (String temp : parser.next().split("\\|")) {
                int index = Integer.valueOf(temp.substring(0, 2), 16);
                double value = Byte.valueOf(temp.substring(2, 4), 16);
                value += (value < 0 ? -0.01 : 0.01) * Integer.valueOf(temp.substring(4), 16);
                position.set(Position.PREFIX_TEMP + index, value);
            }
        }

        return position;
    }

    private List<Position> decodeBinaryMessage(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<>();

        String flag = buf.toString(2, 1, StandardCharsets.US_ASCII);
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');

        String imei = buf.toString(index + 1, 15, StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        buf.skipBytes(index + 1 + 15 + 1 + 3 + 1 + 2 + 2 + 4);

        while (buf.readableBytes() >= 0x34) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            position.setLatitude(buf.readInt() * 0.000001);
            position.setLongitude(buf.readInt() * 0.000001);

            position.setTime(new Date((946684800 + buf.readUnsignedInt()) * 1000)); // 946684800 = 2000-01-01

            position.setValid(buf.readUnsignedByte() == 1);

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            int rssi = buf.readUnsignedByte();

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());

            position.set(Position.KEY_HDOP, buf.readUnsignedShort() * 0.1);

            position.setAltitude(buf.readUnsignedShort());

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            position.set("runtime", buf.readUnsignedInt());

            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                    rssi)));

            position.set(Position.KEY_STATUS, buf.readUnsignedShort());

            position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
            position.set(Position.KEY_POWER, buf.readUnsignedShort());

            buf.readUnsignedInt(); // geo-fence

            positions.add(position);
        }

        if (channel != null) {
            StringBuilder command = new StringBuilder("@@");
            command.append(flag).append(27 + positions.size() / 10).append(",");
            command.append(imei).append(",CCC,").append(positions.size()).append("*");
            int checksum = 0;
            for (int i = 0; i < command.length(); i += 1) {
                checksum += command.charAt(i);
            }
            command.append(String.format("%02x", checksum & 0xff).toUpperCase());
            command.append("\r\n");
            channel.write(command.toString()); // delete processed data
        }

        return positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Find type
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',');
        index = buf.indexOf(index + 1, buf.writerIndex(), (byte) ',');

        String type = buf.toString(index + 1, 3, StandardCharsets.US_ASCII);
        switch (type) {
            case "D03":
                if (channel != null) {
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
                    String imei = Context.getIdentityManager().getById(deviceSession.getDeviceId()).getUniqueId();
                    channel.write("@@O46," + imei + ",D00,camera_picture.jpg,0*00\r\n");
                }
                return null;
            case "CCC":
                return decodeBinaryMessage(channel, remoteAddress, buf);
            default:
                return decodeRegularMessage(channel, remoteAddress, buf);
        }
    }

}
