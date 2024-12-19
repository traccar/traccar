/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class FifotrackProtocolDecoder extends BaseProtocolDecoder {

    private ByteBuf photo;

    public FifotrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("x+,")                       // index
            .expression("[^,]+,")                // type
            .number("(d+)?,")                    // alarm
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("([AV]),")                   // validity
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+),")                // longitude
            .number("(d+),")                     // speed
            .number("(d+),")                     // course
            .number("(-?d+),")                   // altitude
            .number("(d+),")                     // odometer
            .number("(d+),")                     // engine hours
            .number("(x+),")                     // status
            .number("(x+)?,")                    // input
            .number("(x+)?,")                    // output
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cid
            .number("([x|]+)")                   // adc
            .expression(",([^,]+)")              // rfid
            .expression(",([^*]*)").optional(2)  // sensors
            .any()
            .compile();

    private static final Pattern PATTERN_NEW = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("(x+),")                     // index
            .text("A03,")                        // type
            .number("(d+)?,")                    // alarm
            .number("(dd)(dd)(dd)")              // date (yymmdd)
            .number("(dd)(dd)(dd),")             // time (hhmmss)
            .number("(d+)|")                     // mcc
            .number("(d+)|")                     // mnc
            .number("(x+)|")                     // lac
            .number("(x+),")                     // cid
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // battery level
            .number("(x+),")                     // status
            .groupBegin()
            .text("0,")                          // gps location
            .number("([AV]),")                   // validity
            .number("(d+),")                     // speed
            .number("(d+),")                     // satellites
            .number("(-?d+.d+),")                // latitude
            .number("(-?d+.d+)")                 // longitude
            .or()
            .text("1,")                          // wifi location
            .expression("([^*]+)")               // wifi
            .groupEnd()
            .text("*")
            .number("xx")                        // checksum
            .compile();

    private static final Pattern PATTERN_PHOTO = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .any()
            .number(",(d+),")                    // length
            .expression("([^*]+)")               // photo id
            .text("*")
            .number("xx")
            .compile();

    private static final Pattern PATTERN_PHOTO_DATA = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .number("x+,")                       // index
            .expression("[^,]+,")                // type
            .expression("([^,]+),")              // photo id
            .number("(d+),")                     // offset
            .number("(d+),")                     // size
            .compile();

    private static final Pattern PATTERN_RESULT = new PatternBuilder()
            .text("$$")
            .number("d+,")                       // length
            .number("(d+),")                     // imei
            .any()
            .expression(",([A-Z]+)")             // result
            .text("*")
            .number("xx")
            .compile();

    private void sendResponse(Channel channel, SocketAddress remoteAddress, String imei, String content) {
        if (channel != null) {
            int length = 1 + imei.length() + 1 + content.length();
            String response = String.format("##%02d,%s,%s*", length, imei, content);
            response += Checksum.sum(response) + "\r\n";
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
    }

    private void requestPhoto(Channel channel, SocketAddress remoteAddress, String imei, String file) {
        String content = "1,D06," + file + "," + photo.writerIndex() + "," + Math.min(1024, photo.writableBytes());
        sendResponse(channel, remoteAddress, imei, content);
    }

    private String decodeAlarm(Integer alarm) {
        if (alarm != null) {
            return switch (alarm) {
                case 2 -> Position.ALARM_SOS;
                case 14 -> Position.ALARM_LOW_POWER;
                case 15 -> Position.ALARM_POWER_CUT;
                case 16 -> Position.ALARM_POWER_RESTORED;
                case 17 -> Position.ALARM_LOW_BATTERY;
                case 18 -> Position.ALARM_OVERSPEED;
                case 20 -> Position.ALARM_GPS_ANTENNA_CUT;
                case 21 -> Position.ALARM_VIBRATION;
                case 23 -> Position.ALARM_ACCELERATION;
                case 24 -> Position.ALARM_BRAKING;
                case 27 -> Position.ALARM_FATIGUE_DRIVING;
                case 30, 32 -> Position.ALARM_JAMMING;
                case 31 -> Position.ALARM_FALL_DOWN;
                case 33 -> Position.ALARM_GEOFENCE_EXIT;
                case 34 -> Position.ALARM_GEOFENCE_ENTER;
                case 35 -> Position.ALARM_IDLE;
                case 40, 41 -> Position.ALARM_TEMPERATURE;
                case 53 -> Position.ALARM_POWER_ON;
                case 54 -> Position.ALARM_POWER_OFF;
                default -> null;
            };
        }
        return null;
    }


    private Object decodeLocationNew(
            Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_NEW, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        String index = parser.next();

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.addAlarm(decodeAlarm(parser.nextInt()));

        position.setDeviceTime(parser.nextDateTime());

        Network network = new Network();
        network.addCellTower(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt()));

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_BATTERY_LEVEL, parser.nextInt());
        position.set(Position.KEY_STATUS, parser.nextHexInt());

        if (parser.hasNext(5)) {

            position.setValid(parser.next().equals("A"));
            position.setFixTime(position.getDeviceTime());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
            position.set(Position.KEY_SATELLITES, parser.nextInt());
            position.setLatitude(parser.nextDouble());
            position.setLongitude(parser.nextDouble());

        } else {

            getLastLocation(position, position.getDeviceTime());

            String[] points = parser.next().split("\\|");
            for (String point : points) {
                String[] wifi = point.split(":");
                String mac = wifi[0].replaceAll("(..)", "$1:");
                network.addWifiAccessPoint(WifiAccessPoint.from(
                        mac.substring(0, mac.length() - 1), Integer.parseInt(wifi[1])));
            }

        }

        position.setNetwork(network);

        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String response = index + ",A03," + dateFormat.format(new Date());
        sendResponse(channel, remoteAddress, imei, response);

        return position;
    }

    private Object decodeLocation(
            Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.addAlarm(decodeAlarm(parser.nextInt()));

        position.setTime(parser.nextDateTime());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextInt()));
        position.setCourse(parser.nextInt());
        position.setAltitude(parser.nextInt());

        position.set(Position.KEY_ODOMETER, parser.nextLong());
        position.set(Position.KEY_HOURS, parser.nextLong() * 1000);

        long status = parser.nextHexLong();
        position.set(Position.KEY_RSSI, BitUtil.between(status, 3, 8));
        position.set(Position.KEY_SATELLITES, BitUtil.from(status, 28));
        position.set(Position.KEY_STATUS, status);

        position.set(Position.KEY_INPUT, parser.nextHexInt());
        position.set(Position.KEY_OUTPUT, parser.nextHexInt());

        position.setNetwork(new Network(CellTower.from(
                parser.nextInt(), parser.nextInt(), parser.nextHexInt(), parser.nextHexInt())));

        String[] adc = parser.next().split("\\|");
        for (int i = 0; i < adc.length; i++) {
            position.set(Position.PREFIX_ADC + (i + 1), Integer.parseInt(adc[i], 16));
        }

        if (parser.hasNext()) {
            String value = parser.next();
            if (value.matches("\\p{XDigit}+")) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(Integer.parseInt(value, 16)));
            } else {
                position.set(Position.KEY_CARD, value);
            }
        }

        if (parser.hasNext()) {
            String[] sensors = parser.next().split("\\|");
            for (int i = 0; i < sensors.length; i++) {
                position.set(Position.PREFIX_IO + (i + 1), sensors[i]);
            }
        }

        return position;
    }

    private Object decodeResult(
            Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN_RESULT, sentence);
        if (!parser.matches()) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_RESULT, parser.next());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        int typeIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) ',') + 1;
        typeIndex = buf.indexOf(typeIndex, buf.writerIndex(), (byte) ',') + 1;
        typeIndex = buf.indexOf(typeIndex, buf.writerIndex(), (byte) ',') + 1;
        String type = buf.toString(typeIndex, 3, StandardCharsets.US_ASCII);

        if (type.startsWith("B")) {

            return decodeResult(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));

        } else if (type.equals("D05")) {

            String sentence = buf.toString(StandardCharsets.US_ASCII);
            Parser parser = new Parser(PATTERN_PHOTO, sentence);
            if (parser.matches()) {
                String imei = parser.next();
                int length = parser.nextInt();
                String photoId = parser.next();
                photo = Unpooled.buffer(length);
                requestPhoto(channel, remoteAddress, imei, photoId);
            }

        } else if (type.equals("D06")) {

            if (photo == null) {
                return null;
            }
            int dataIndex = buf.indexOf(typeIndex + 4, buf.writerIndex(), (byte) ',') + 1;
            dataIndex = buf.indexOf(dataIndex, buf.writerIndex(), (byte) ',') + 1;
            dataIndex = buf.indexOf(dataIndex, buf.writerIndex(), (byte) ',') + 1;
            String sentence = buf.toString(buf.readerIndex(), dataIndex, StandardCharsets.US_ASCII);
            Parser parser = new Parser(PATTERN_PHOTO_DATA, sentence);
            if (parser.matches()) {
                String imei = parser.next();
                String photoId = parser.next();
                parser.nextInt(); // offset
                parser.nextInt(); // size
                buf.readerIndex(dataIndex);
                buf.readBytes(photo, buf.readableBytes() - 3); // ignore checksum
                if (photo.isWritable()) {
                    requestPhoto(channel, remoteAddress, imei, photoId);
                } else {
                    Position position = new Position(getProtocolName());
                    position.setDeviceId(getDeviceSession(channel, remoteAddress, imei).getDeviceId());
                    getLastLocation(position, null);
                    position.set(Position.KEY_IMAGE, writeMediaFile(imei, photo, "jpg"));
                    photo.release();
                    photo = null;
                    return position;
                }
            }

        } else if (type.equals("A03")) {

            return decodeLocationNew(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));

        } else {

            return decodeLocation(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));

        }

        return null;
    }

}
