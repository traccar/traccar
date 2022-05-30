/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.ObdDecoder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class UlbotechProtocolDecoder extends BaseProtocolDecoder {

    public UlbotechProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final short DATA_GPS = 0x01;
    private static final short DATA_LBS = 0x02;
    private static final short DATA_STATUS = 0x03;
    private static final short DATA_ODOMETER = 0x04;
    private static final short DATA_ADC = 0x05;
    private static final short DATA_GEOFENCE = 0x06;
    private static final short DATA_OBD2 = 0x07;
    private static final short DATA_FUEL = 0x08;
    private static final short DATA_OBD2_ALARM = 0x09;
    private static final short DATA_HARSH_DRIVER = 0x0A;
    private static final short DATA_CANBUS = 0x0B;
    private static final short DATA_J1708 = 0x0C;
    private static final short DATA_VIN = 0x0D;
    private static final short DATA_RFID = 0x0E;
    private static final short DATA_EVENT = 0x10;

    private void decodeObd(Position position, ByteBuf buf, int length) {

        int end = buf.readerIndex() + length;

        while (buf.readerIndex() < end) {
            int parameterLength = buf.getUnsignedByte(buf.readerIndex()) >> 4;
            int mode = buf.readUnsignedByte() & 0x0F;
            position.add(ObdDecoder.decode(mode, ByteBufUtil.hexDump(buf.readSlice(parameterLength - 1))));
        }
    }

    private void decodeJ1708(Position position, ByteBuf buf, int length) {

        int end = buf.readerIndex() + length;

        while (buf.readerIndex() < end) {
            int mark = buf.readUnsignedByte();
            int len = BitUtil.between(mark, 0, 6);
            int type = BitUtil.between(mark, 6, 8);
            int id = buf.readUnsignedByte();
            if (type == 3) {
                id += 256;
            }
            String value = ByteBufUtil.hexDump(buf.readSlice(len - 1));
            if (type == 2 || type == 3) {
                position.set("pid" + id, value);
            }
        }
    }

    private void decodeDriverBehavior(Position position, ByteBuf buf) {

        int value = buf.readUnsignedByte();

        if (BitUtil.check(value, 0)) {
            position.set("rapidAcceleration", true);
        }
        if (BitUtil.check(value, 1)) {
            position.set("roughBraking", true);
        }
        if (BitUtil.check(value, 2)) {
            position.set("harshCourse", true);
        }
        if (BitUtil.check(value, 3)) {
            position.set("noWarmUp", true);
        }
        if (BitUtil.check(value, 4)) {
            position.set("longIdle", true);
        }
        if (BitUtil.check(value, 5)) {
            position.set("fatigueDriving", true);
        }
        if (BitUtil.check(value, 6)) {
            position.set("roughTerrain", true);
        }
        if (BitUtil.check(value, 7)) {
            position.set("highRpm", true);
        }
    }

    private String decodeAlarm(int alarm) {
        if (BitUtil.check(alarm, 0)) {
            return Position.ALARM_POWER_OFF;
        }
        if (BitUtil.check(alarm, 1)) {
            return Position.ALARM_MOVEMENT;
        }
        if (BitUtil.check(alarm, 2)) {
            return Position.ALARM_OVERSPEED;
        }
        if (BitUtil.check(alarm, 4)) {
            return Position.ALARM_GEOFENCE;
        }
        if (BitUtil.check(alarm, 10)) {
            return Position.ALARM_SOS;
        }
        return null;
    }

    private void decodeAdc(Position position, ByteBuf buf, int length) {
        for (int i = 0; i < length / 2; i++) {
            int value = buf.readUnsignedShort();
            int id = BitUtil.from(value, 12);
            value = BitUtil.to(value, 12);
            switch (id) {
                case 0:
                    position.set(Position.KEY_POWER, value * (100 + 10) / 4096.0 - 10);
                    break;
                case 1:
                    position.set(Position.PREFIX_TEMP + 1, value * (125 + 55) / 4096.0 - 55);
                    break;
                case 2:
                    position.set(Position.KEY_BATTERY, value * (100 + 10) / 4096.0 - 10);
                    break;
                case 3:
                    position.set(Position.PREFIX_ADC + 1, value * (100 + 10) / 4096.0 - 10);
                    break;
                default:
                    position.set(Position.PREFIX_IO + id, value);
                    break;
            }
        }
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("*TS")
            .number("dd,")                       // protocol version
            .number("(d{15}),")                  // device id
            .number("(dd)(dd)(dd)")              // time
            .number("(dd)(dd)(dd),")             // date
            .expression("([^#]+)")               // command
            .text("#")
            .compile();

    private Object decodeText(Channel channel, SocketAddress remoteAddress, String sentence) {

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

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0))
                .setDateReverse(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0));

        getLastLocation(position, dateBuilder.getDate());

        position.set(Position.KEY_RESULT, parser.next());

        return position;
    }

    private Object decodeBinary(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        buf.readUnsignedByte(); // header
        buf.readUnsignedByte(); // version
        buf.readUnsignedByte(); // type

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
            deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId()));
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        TimeZone timeZone = deviceSession.get(DeviceSession.KEY_TIMEZONE);
        long seconds = buf.readUnsignedInt() & 0x7fffffffL;
        seconds += 946684800L; // 2000-01-01 00:00
        seconds -= timeZone.getRawOffset() / 1000;
        Date time = new Date(seconds * 1000);

        boolean hasLocation = false;

        while (buf.readableBytes() > 3) {

            int type = buf.readUnsignedByte();
            int length = type == DATA_CANBUS ? buf.readUnsignedShort() : buf.readUnsignedByte();

            switch (type) {

                case DATA_GPS:
                    hasLocation = true;
                    position.setLatitude(buf.readInt() / 1000000.0);
                    position.setLongitude(buf.readInt() / 1000000.0);
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    position.setCourse(buf.readUnsignedShort());
                    int hdop = buf.readUnsignedShort();
                    position.setValid(hdop < 9999);
                    position.set(Position.KEY_HDOP, hdop * 0.01);
                    break;

                case DATA_LBS:
                    if (length == 11) {
                        position.setNetwork(new Network(CellTower.from(
                                buf.readUnsignedShort(), buf.readUnsignedShort(),
                                buf.readUnsignedShort(), buf.readUnsignedInt(), -buf.readUnsignedByte())));
                    } else {
                        position.setNetwork(new Network(CellTower.from(
                                buf.readUnsignedShort(), buf.readUnsignedShort(),
                                buf.readUnsignedShort(), buf.readUnsignedShort(), -buf.readUnsignedByte())));
                    }
                    if (length > 9 && length != 11) {
                        buf.skipBytes(length - 9);
                    }
                    break;

                case DATA_STATUS:
                    int status = buf.readUnsignedShort();
                    position.set(Position.KEY_IGNITION, BitUtil.check(status, 9));
                    position.set(Position.KEY_STATUS, status);
                    position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedShort()));
                    break;

                case DATA_ODOMETER:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    break;

                case DATA_ADC:
                    decodeAdc(position, buf, length);
                    break;

                case DATA_GEOFENCE:
                    position.set("geofenceIn", buf.readUnsignedInt());
                    position.set("geofenceAlarm", buf.readUnsignedInt());
                    break;

                case DATA_OBD2:
                    decodeObd(position, buf, length);
                    break;

                case DATA_FUEL:
                    position.set(Position.KEY_FUEL_CONSUMPTION, buf.readUnsignedInt() / 10000.0);
                    break;

                case DATA_OBD2_ALARM:
                    decodeObd(position, buf, length);
                    break;

                case DATA_HARSH_DRIVER:
                    decodeDriverBehavior(position, buf);
                    break;

                case DATA_CANBUS:
                    position.set("can", ByteBufUtil.hexDump(buf.readSlice(length)));
                    break;

                case DATA_J1708:
                    decodeJ1708(position, buf, length);
                    break;

                case DATA_VIN:
                    position.set(Position.KEY_VIN, buf.readSlice(length).toString(StandardCharsets.US_ASCII));
                    break;

                case DATA_RFID:
                    position.set(Position.KEY_DRIVER_UNIQUE_ID,
                            buf.readSlice(length - 1).toString(StandardCharsets.US_ASCII));
                    position.set("authorized", buf.readUnsignedByte() != 0);
                    break;

                case DATA_EVENT:
                    position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                    if (length > 1) {
                        position.set("eventMask", buf.readUnsignedInt());
                    }
                    break;

                default:
                    buf.skipBytes(length);
                    break;
            }
        }

        if (!hasLocation) {
            getLastLocation(position, time);
        } else {
            position.setTime(time);
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (buf.getUnsignedByte(buf.readerIndex()) == 0xF8) {

            if (channel != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeByte(0xF8);
                response.writeByte(DATA_GPS);
                response.writeByte(0xFE);
                response.writeShort(buf.getShort(response.writerIndex() - 1 - 2));
                response.writeShort(Checksum.crc16(Checksum.CRC16_XMODEM, response.nioBuffer(1, 4)));
                response.writeByte(0xF8);
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

            return decodeBinary(channel, remoteAddress, buf);
        } else {

            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(Unpooled.copiedBuffer(String.format("*TS01,ACK:%04X#",
                        Checksum.crc16(Checksum.CRC16_XMODEM, buf.nioBuffer(1, buf.writerIndex() - 2))),
                        StandardCharsets.US_ASCII), remoteAddress));
            }

            return decodeText(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII));
        }
    }

}
