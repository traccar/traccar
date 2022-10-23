/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MeiligaoProtocolDecoder extends BaseProtocolDecoder {

    private final Map<Byte, ByteBuf> photos = new HashMap<>();

    public MeiligaoProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(d+)(dd)(dd).?d*,")         // time (hhmmss)
            .expression("([AV]),")               // validity
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW]),")
            .number("(d+.?d*)?,")                // speed
            .number("(d+.?d*)?,")                // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .expression("[^\\|]*")
            .groupBegin()
            .number("|(d+.d+)?")                 // hdop
            .number("|(-?d+.?d*)?")              // altitude
            .number("|(xxxx)?")                  // state
            .groupBegin()
            .number("|(xxxx),(xxxx)")            // adc
            .number(",(xxxx)").optional()
            .number(",(xxxx)").optional()
            .number(",(xxxx)").optional()
            .number(",(xxxx)").optional()
            .number(",(xxxx)").optional()
            .number(",(xxxx)").optional()
            .groupBegin()
            .number("|x{16,20}")                 // cell
            .number("|(xx)")                     // rssi
            .number("|(x{8})")                   // odometer
            .groupBegin()
            .number("|(xx)")                     // satellites
            .groupBegin()
            .text("|")
            .expression("(.*)")                  // driver
            .groupEnd("?")
            .groupEnd("?")
            .or()
            .number("|(d{1,9})")                 // odometer
            .groupBegin()
            .number("|(x{5,})")                  // rfid
            .groupEnd("?")
            .groupEnd("?")
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_RFID = new PatternBuilder()
            .number("|(dd)(dd)(dd),")            // time (hhmmss)
            .number("(dd)(dd)(dd),")             // date (ddmmyy)
            .number("(d+)(dd.d+),")              // latitude
            .expression("([NS]),")
            .number("(d+)(dd.d+),")              // longitude
            .expression("([EW])")
            .compile();

    private static final Pattern PATTERN_OBD = new PatternBuilder()
            .number("(d+.d+),")                  // battery
            .number("(d+),")                     // rpm
            .number("(d+),")                     // speed
            .number("(d+.d+),")                  // throttle
            .number("(d+.d+),")                  // engine load
            .number("(-?d+),")                   // coolant temp
            .number("(d+.d+),")                  // instantaneous fuel
            .number("(d+.d+),")                  // average fuel
            .number("(d+.d+),")                  // driving range
            .number("(d+.?d*),")                 // odometer
            .number("(d+.d+),")                  // single fuel consumption
            .number("(d+.d+),")                  // total fuel consumption
            .number("(d+),")                     // error code count
            .number("(d+),")                     // hard acceleration count
            .number("(d+)")                      // hard brake count
            .compile();

    private static final Pattern PATTERN_OBDA = new PatternBuilder()
            .number("(d+),")                     // total ignition
            .number("(d+.d+),")                  // total driving time
            .number("(d+.d+),")                  // total idling time
            .number("(d+),")                     // average hot start time
            .number("(d+),")                     // average speed
            .number("(d+),")                     // history highest speed
            .number("(d+),")                     // history highest rpm
            .number("(d+),")                     // total hard acceleration
            .number("(d+)")                      // total hard brake
            .compile();

    public static final int MSG_HEARTBEAT = 0x0001;
    public static final int MSG_SERVER = 0x0002;
    public static final int MSG_LOGIN = 0x5000;
    public static final int MSG_LOGIN_RESPONSE = 0x4000;
    public static final int MSG_POSITION = 0x9955;
    public static final int MSG_POSITION_LOGGED = 0x9016;
    public static final int MSG_ALARM = 0x9999;
    public static final int MSG_RFID = 0x9966;
    public static final int MSG_RETRANSMISSION = 0x6688;

    public static final int MSG_OBD_RT = 0x9901;
    public static final int MSG_OBD_RTA = 0x9902;

    public static final int MSG_TRACK_ON_DEMAND = 0x4101;
    public static final int MSG_TRACK_BY_INTERVAL = 0x4102;
    public static final int MSG_MOVEMENT_ALARM = 0x4106;
    public static final int MSG_OUTPUT_CONTROL_1 = 0x4114;
    public static final int MSG_OUTPUT_CONTROL_2 = 0x4115;
    public static final int MSG_TIME_ZONE = 0x4132;
    public static final int MSG_TAKE_PHOTO = 0x4151;
    public static final int MSG_UPLOAD_PHOTO = 0x0800;
    public static final int MSG_UPLOAD_PHOTO_RESPONSE = 0x8801;
    public static final int MSG_DATA_PHOTO = 0x9988;
    public static final int MSG_POSITION_IMAGE = 0x9977;
    public static final int MSG_UPLOAD_COMPLETE = 0x0f80;
    public static final int MSG_REBOOT_GPS = 0x4902;

    private DeviceSession identify(ByteBuf buf, Channel channel, SocketAddress remoteAddress) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            int b = buf.readUnsignedByte();

            // First digit
            int d1 = (b & 0xf0) >> 4;
            if (d1 == 0xf) {
                break;
            }
            builder.append(d1);

            // Second digit
            int d2 = b & 0x0f;
            if (d2 == 0xf) {
                break;
            }
            builder.append(d2);
        }

        String id = builder.toString();

        if (id.length() == 14) {
            return getDeviceSession(channel, remoteAddress, id, id + Checksum.luhn(Long.parseLong(id)));
        } else {
            return getDeviceSession(channel, remoteAddress, id);
        }
    }

    private static void sendResponse(
            Channel channel, SocketAddress remoteAddress, ByteBuf id, int type, ByteBuf msg) {

        if (channel != null) {
            ByteBuf buf = Unpooled.buffer(
                    2 + 2 + id.readableBytes() + 2 + msg.readableBytes() + 2 + 2);

            buf.writeByte('@');
            buf.writeByte('@');
            buf.writeShort(buf.capacity());
            buf.writeBytes(id);
            buf.writeShort(type);
            buf.writeBytes(msg);
            msg.release();
            buf.writeShort(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.nioBuffer()));
            buf.writeByte('\r');
            buf.writeByte('\n');

            channel.writeAndFlush(new NetworkMessage(buf, remoteAddress));
        }
    }

    private String decodeAlarm(short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x10:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_OVERSPEED;
            case 0x12:
                return Position.ALARM_MOVEMENT;
            case 0x13:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x14:
                return Position.ALARM_ACCIDENT;
            case 0x50:
                return Position.ALARM_POWER_OFF;
            case 0x53:
                return Position.ALARM_GPS_ANTENNA_CUT;
            case 0x72:
                return Position.ALARM_BRAKING;
            case 0x73:
                return Position.ALARM_ACCELERATION;
            default:
                return null;
        }
    }

    private Position decodeRegular(Position position, String sentence) {
        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        if (parser.hasNext()) {
            position.setSpeed(parser.nextDouble(0));
        }

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble(0));
        }

        dateBuilder.setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_HDOP, parser.nextDouble());

        if (parser.hasNext()) {
            position.setAltitude(parser.nextDouble(0));
        }

        if (parser.hasNext()) {
            int status = parser.nextHexInt();
            for (int i = 1; i <= 5; i++) {
                position.set(Position.PREFIX_OUT + i, BitUtil.check(status, i - 1));
            }
            for (int i = 1; i <= 5; i++) {
                position.set(Position.PREFIX_IN + i, BitUtil.check(status, i - 1 + 8));
            }
        }

        for (int i = 1; i <= 8; i++) {
            position.set(Position.PREFIX_ADC + i, parser.nextHexInt());
        }

        position.set(Position.KEY_RSSI, parser.nextHexInt());
        position.set(Position.KEY_ODOMETER, parser.nextHexLong());
        position.set(Position.KEY_SATELLITES, parser.nextHexInt());
        position.set("driverLicense", parser.next());
        position.set(Position.KEY_ODOMETER, parser.nextLong());
        position.set(Position.KEY_DRIVER_UNIQUE_ID, parser.next());

        return position;
    }

    private Position decodeRfid(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_RFID, sentence);
        if (!parser.matches()) {
            return null;
        }

        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.HMS_DMY));

        position.setValid(true);
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        return position;
    }

    private Position decodeObd(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_OBD, sentence);
        if (!parser.matches()) {
            return null;
        }

        getLastLocation(position, null);

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.KEY_RPM, parser.nextInt());
        position.set(Position.KEY_OBD_SPEED, parser.nextInt());
        position.set(Position.KEY_THROTTLE, parser.nextDouble());
        position.set(Position.KEY_ENGINE_LOAD, parser.nextDouble());
        position.set(Position.KEY_COOLANT_TEMP, parser.nextInt());
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextDouble());
        position.set("averageFuelConsumption", parser.nextDouble());
        position.set("drivingRange", parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set("singleFuelConsumption", parser.nextDouble());
        position.set(Position.KEY_FUEL_USED, parser.nextDouble());
        position.set(Position.KEY_DTCS, parser.nextInt());
        position.set("hardAccelerationCount", parser.nextInt());
        position.set("hardBrakingCount", parser.nextInt());

        return position;
    }

    private Position decodeObdA(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_OBDA, sentence);
        if (!parser.matches()) {
            return null;
        }

        getLastLocation(position, null);

        position.set("totalIgnitionNo", parser.nextInt(0));
        position.set("totalDrivingTime", parser.nextDouble(0));
        position.set("totalIdlingTime", parser.nextDouble(0));
        position.set("averageHotStartTime", parser.nextInt(0));
        position.set("averageSpeed", parser.nextInt(0));
        position.set("historyHighestSpeed", parser.nextInt(0));
        position.set("historyHighestRpm", parser.nextInt(0));
        position.set("totalHarshAccerleration", parser.nextInt(0));
        position.set("totalHarshBrake", parser.nextInt(0));

        return position;
    }

    private List<Position> decodeRetransmission(ByteBuf buf, DeviceSession deviceSession) {
        List<Position> positions = new LinkedList<>();

        int count = buf.readUnsignedByte();
        for (int i = 0; i < count; i++) {

            buf.readUnsignedByte(); // alarm

            int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '\\');
            if (endIndex < 0) {
                endIndex = buf.writerIndex() - 4;
            }

            String sentence = buf.readSlice(endIndex - buf.readerIndex()).toString(StandardCharsets.US_ASCII);

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            position = decodeRegular(position, sentence);

            if (position != null) {
                positions.add(position);
            }

            if (buf.readableBytes() > 4) {
                buf.readUnsignedByte(); // delimiter
            }

        }

        return positions;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        buf.skipBytes(2); // header
        buf.readShort(); // length
        ByteBuf id = buf.readSlice(7);
        int command = buf.readUnsignedShort();

        if (command == MSG_LOGIN) {
            ByteBuf response = Unpooled.wrappedBuffer(new byte[]{0x01});
            sendResponse(channel, remoteAddress, id, MSG_LOGIN_RESPONSE, response);
            return null;
        } else if (command == MSG_HEARTBEAT) {
            ByteBuf response = Unpooled.wrappedBuffer(new byte[]{0x01});
            sendResponse(channel, remoteAddress, id, MSG_HEARTBEAT, response);
            return null;
        } else if (command == MSG_SERVER) {
            ByteBuf response = Unpooled.copiedBuffer(getServer(channel, ':'), StandardCharsets.US_ASCII);
            sendResponse(channel, remoteAddress, id, MSG_SERVER, response);
            return null;
        } else if (command == MSG_UPLOAD_PHOTO) {
            byte imageIndex = buf.readByte();
            photos.put(imageIndex, Unpooled.buffer());
            ByteBuf response = Unpooled.copiedBuffer(new byte[]{imageIndex});
            sendResponse(channel, remoteAddress, id, MSG_UPLOAD_PHOTO_RESPONSE, response);
            return null;
        } else if (command == MSG_UPLOAD_COMPLETE) {
            byte imageIndex = buf.readByte();
            ByteBuf response = Unpooled.copiedBuffer(new byte[]{imageIndex, 0, 0});
            sendResponse(channel, remoteAddress, id, MSG_RETRANSMISSION, response);
            return null;
        }

        DeviceSession deviceSession = identify(id, channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (command == MSG_DATA_PHOTO) {

            byte imageIndex = buf.readByte();
            buf.readUnsignedShort(); // image footage
            buf.readUnsignedByte(); // total packets
            buf.readUnsignedByte(); // packet index

            photos.get(imageIndex).writeBytes(buf, buf.readableBytes() - 2 - 2);

            return null;

        } else if (command == MSG_RETRANSMISSION) {

            return decodeRetransmission(buf, deviceSession);

        } else {

            Position position = new Position(getProtocolName());

            position.setDeviceId(deviceSession.getDeviceId());

            if (command == MSG_ALARM) {
                short alarmCode = buf.readUnsignedByte();
                position.set(Position.KEY_ALARM, decodeAlarm(alarmCode));
                if (alarmCode >= 0x02 && alarmCode <= 0x05) {
                    position.set(Position.PREFIX_IN + alarmCode, 1);
                } else if (alarmCode >= 0x32 && alarmCode <= 0x35) {
                    position.set(Position.PREFIX_IN + (alarmCode - 0x30), 0);
                }
            } else if (command == MSG_POSITION_LOGGED) {
                buf.skipBytes(6);
            } else if (command == MSG_RFID) {
                for (int i = 0; i < 15; i++) {
                    long rfid = buf.readUnsignedInt();
                    if (rfid != 0) {
                        String card = String.format("%010d", rfid);
                        position.set("card" + (i + 1), card);
                        position.set(Position.KEY_DRIVER_UNIQUE_ID, card);
                    }
                }
            } else if (command == MSG_POSITION_IMAGE) {
                byte imageIndex = buf.readByte();
                buf.readUnsignedByte(); // image upload type
                ByteBuf photo = photos.remove(imageIndex);
                try {
                    position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), photo, "jpg"));
                } finally {
                    photo.release();
                }
            }

            String sentence = buf.toString(buf.readerIndex(), buf.readableBytes() - 4, StandardCharsets.US_ASCII);

            switch (command) {
                case MSG_POSITION:
                case MSG_POSITION_LOGGED:
                case MSG_ALARM:
                case MSG_POSITION_IMAGE:
                    return decodeRegular(position, sentence);
                case MSG_RFID:
                    return decodeRfid(position, sentence);
                case MSG_OBD_RT:
                    return decodeObd(position, sentence);
                case MSG_OBD_RTA:
                    return decodeObdA(position, sentence);
                default:
                    return null;
            }

        }
    }

}
