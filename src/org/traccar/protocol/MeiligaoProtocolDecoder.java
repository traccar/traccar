/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class MeiligaoProtocolDecoder extends BaseProtocolDecoder {

    public MeiligaoProtocolDecoder(MeiligaoProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .number("(dd)(dd)(dd).?(d+)?,")      // time
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
            .number("(?:,(xxxx),(xxxx),(xxxx),(xxxx),(xxxx),(xxxx))?")
            .groupBegin()
            .number("|x{16}")                    // cell
            .number("|(xx)")                     // gsm
            .number("|(x{8})")                   // odometer
            .or()
            .number("|(x{9})")                   // odometer
            .groupBegin()
            .number("|(x{5,})")                  // rfid
            .groupEnd("?")
            .groupEnd("?")
            .groupEnd("?")
            .groupEnd("?")
            .any()
            .compile();

    private static final Pattern PATTERN_RFID = new PatternBuilder()
            .number("|(dd)(dd)(dd),")            // time
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
            .number("(d+),")                     // harsh acceleration count
            .number("(d+)")                      // harsh break count
            .compile();

    private static final Pattern PATTERN_OBDA = new PatternBuilder()
            .number("(d+),")                     // total ignition
            .number("(d+.d+),")                  // total driving time
            .number("(d+.d+),")                  // total idling time
            .number("(d+),")                     // average hot start time
            .number("(d+),")                     // average speed
            .number("(d+),")                     // history highest speed
            .number("(d+),")                     // history highest rpm
            .number("(d+),")                     // total harsh acceleration
            .number("(d+)")                      // total harsh break n0
            .compile();

    public static final int MSG_HEARTBEAT = 0x0001;
    public static final int MSG_SERVER = 0x0002;
    public static final int MSG_LOGIN = 0x5000;
    public static final int MSG_LOGIN_RESPONSE = 0x4000;
    public static final int MSG_POSITION = 0x9955;
    public static final int MSG_POSITION_LOGGED = 0x9016;
    public static final int MSG_ALARM = 0x9999;
    public static final int MSG_RFID = 0x9966;

    public static final int MSG_OBD_RT = 0x9901;
    public static final int MSG_OBD_RTA = 0x9902;

    private DeviceSession identify(ChannelBuffer buf, Channel channel, SocketAddress remoteAddress) {
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
            Channel channel, SocketAddress remoteAddress, ChannelBuffer id, int type, ChannelBuffer msg) {

        if (channel != null) {
            ChannelBuffer buf = ChannelBuffers.buffer(
                    2 + 2 + id.readableBytes() + 2 + msg.readableBytes() + 2 + 2);

            buf.writeByte('@');
            buf.writeByte('@');
            buf.writeShort(buf.capacity());
            buf.writeBytes(id);
            buf.writeShort(type);
            buf.writeBytes(msg);
            buf.writeShort(Checksum.crc16(Checksum.CRC16_CCITT_FALSE, buf.toByteBuffer()));
            buf.writeByte('\r');
            buf.writeByte('\n');

            channel.write(buf, remoteAddress);
        }
    }

    private String getServer(Channel channel) {
        String server = Context.getConfig().getString(getProtocolName() + ".server");
        if (server == null) {
            InetSocketAddress address = (InetSocketAddress) channel.getLocalAddress();
            server = address.getAddress().getHostAddress() + ":" + address.getPort();
        }
        return server;
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
            case 0x50:
                return Position.ALARM_POWER_OFF;
            case 0x53:
                return Position.ALARM_GPS_ANTENNA_CUT;
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
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        if (parser.hasNext()) {
            dateBuilder.setMillis(parser.nextInt());
        }

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());

        if (parser.hasNext()) {
            position.setSpeed(parser.nextDouble());
        }

        if (parser.hasNext()) {
            position.setCourse(parser.nextDouble());
        }

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set(Position.KEY_HDOP, parser.next());

        if (parser.hasNext()) {
            position.setAltitude(parser.nextDouble());
        }

        position.set(Position.KEY_STATUS, parser.next());

        for (int i = 1; i <= 8; i++) {
            if (parser.hasNext()) {
                position.set(Position.PREFIX_ADC + i, parser.nextInt(16));
            }
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_RSSI, parser.nextInt(16));
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextLong(16));
        }
        if (parser.hasNext()) {
            position.set(Position.KEY_ODOMETER, parser.nextLong(16));
        }

        if (parser.hasNext()) {
            position.set(Position.KEY_RFID, parser.nextInt(16));
        }

        return position;
    }

    private Position decodeRfid(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_RFID, sentence);
        if (!parser.matches()) {
            return null;
        }

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

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
        position.set("engineLoad", parser.nextDouble());
        position.set(Position.PREFIX_TEMP + 1, parser.nextInt());
        position.set(Position.KEY_FUEL_CONSUMPTION, parser.nextDouble());
        position.set("averageFuelConsumition", parser.nextDouble());
        position.set("drivingRange", parser.nextDouble());
        position.set(Position.KEY_ODOMETER, parser.nextDouble());
        position.set("singleFuelConsumption", parser.nextDouble());
        position.set("totalFuelConsumption", parser.nextDouble());
        position.set(Position.KEY_DTCS, parser.nextInt());
        position.set("harshAcelerationNo", parser.nextInt());
        position.set("harshBreakerNo", parser.nextInt());

        return position;
    }

    private Position decodeObdA(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_OBDA, sentence);
        if (!parser.matches()) {
            return null;
        }

        getLastLocation(position, null);

        position.set("totalIgnitionNo", parser.nextInt());
        position.set("totalDrivingTime", parser.nextDouble());
        position.set("totalIdlingTime", parser.nextDouble());
        position.set("averageHotStartTime", parser.nextInt());
        position.set("averageSpeed", parser.nextInt());
        position.set("historyHighestSpeed", parser.nextInt());
        position.set("historyHighestRpm", parser.nextInt());
        position.set("totalHarshAccerleration", parser.nextInt());
        position.set("totalHarshBrake", parser.nextInt());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        buf.skipBytes(2); // header
        buf.readShort(); // length
        ChannelBuffer id = buf.readBytes(7);
        int command = buf.readUnsignedShort();
        ChannelBuffer response;

        if (channel != null) {
            if (command == MSG_LOGIN) {
                response = ChannelBuffers.wrappedBuffer(new byte[]{0x01});
                sendResponse(channel, remoteAddress, id, MSG_LOGIN_RESPONSE, response);
                return null;
            } else if (command == MSG_HEARTBEAT) {
                response = ChannelBuffers.wrappedBuffer(new byte[]{0x01});
                sendResponse(channel, remoteAddress, id, MSG_HEARTBEAT, response);
                return null;
            } else if (command == MSG_SERVER) {
                response = ChannelBuffers.copiedBuffer(getServer(channel), StandardCharsets.US_ASCII);
                sendResponse(channel, remoteAddress, id, MSG_SERVER, response);
                return null;
            }
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (command == MSG_ALARM) {
            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
        } else if (command == MSG_POSITION_LOGGED) {
            buf.skipBytes(6);
        }

        DeviceSession deviceSession = identify(id, channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }
        position.setDeviceId(deviceSession.getDeviceId());

        if (command == MSG_RFID) {
            for (int i = 0; i < 15; i++) {
                long rfid = buf.readUnsignedInt();
                if (rfid != 0) {
                    String card = String.format("%010d", rfid);
                    position.set("card" + (i + 1), card);
                    position.set(Position.KEY_RFID, card);
                }
            }
        }

        String sentence = buf.toString(buf.readerIndex(), buf.readableBytes() - 4, StandardCharsets.US_ASCII);

        if (command == MSG_POSITION || command == MSG_POSITION_LOGGED || command == MSG_ALARM) {
            return decodeRegular(position, sentence);
        } else if (command == MSG_RFID) {
            return decodeRfid(position, sentence);
        } else if (command == MSG_OBD_RT) {
            return decodeObd(position, sentence);
        } else if (command == MSG_OBD_RTA) {
            return decodeObdA(position, sentence);
        }

        return null;
    }

}
