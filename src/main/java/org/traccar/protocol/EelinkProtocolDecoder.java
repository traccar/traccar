/*
 * Copyright 2014 - 2022 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.socket.DatagramChannel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class EelinkProtocolDecoder extends BaseProtocolDecoder {

    public EelinkProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_STATE = 0x05;
    public static final int MSG_SMS = 0x06;
    public static final int MSG_OBD = 0x07;
    public static final int MSG_DOWNLINK = 0x80;
    public static final int MSG_DATA = 0x81;

    public static final int MSG_NORMAL = 0x12;
    public static final int MSG_WARNING = 0x14;
    public static final int MSG_REPORT = 0x15;
    public static final int MSG_COMMAND = 0x16;
    public static final int MSG_OBD_DATA = 0x17;
    public static final int MSG_OBD_BODY = 0x18;
    public static final int MSG_OBD_CODE = 0x19;
    public static final int MSG_CAMERA_INFO = 0x1E;
    public static final int MSG_CAMERA_DATA = 0x1F;

    private String decodeAlarm(Short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_POWER_OFF;
            case 0x02:
                return Position.ALARM_SOS;
            case 0x03:
                return Position.ALARM_LOW_BATTERY;
            case 0x04:
                return Position.ALARM_VIBRATION;
            case 0x08:
            case 0x09:
                return Position.ALARM_GPS_ANTENNA_CUT;
            case 0x25:
                return Position.ALARM_REMOVING;
            case 0x81:
                return Position.ALARM_LOW_SPEED;
            case 0x82:
                return Position.ALARM_OVERSPEED;
            case 0x83:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x84:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x85:
                return Position.ALARM_ACCIDENT;
            case 0x86:
                return Position.ALARM_FALL_DOWN;
            default:
                return null;
        }
    }

    private void decodeStatus(Position position, int status) {
        if (BitUtil.check(status, 1)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 2));
        }
        if (BitUtil.check(status, 3)) {
            position.set(Position.KEY_ARMED, BitUtil.check(status, 4));
        }
        if (BitUtil.check(status, 5)) {
            position.set(Position.KEY_BLOCKED, !BitUtil.check(status, 6));
        }
        if (BitUtil.check(status, 7)) {
            position.set(Position.KEY_CHARGE, BitUtil.check(status, 8));
        }
        position.set(Position.KEY_STATUS, status);
    }

    private Position decodeOld(DeviceSession deviceSession, ByteBuf buf, int type, int index) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, index);

        position.setTime(new Date(buf.readUnsignedInt() * 1000));
        position.setLatitude(buf.readInt() / 1800000.0);
        position.setLongitude(buf.readInt() / 1800000.0);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());

        position.setNetwork(new Network(CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedMedium())));

        position.setValid((buf.readUnsignedByte() & 0x01) != 0);

        if (type == MSG_GPS) {

            if (buf.readableBytes() >= 2) {
                decodeStatus(position, buf.readUnsignedShort());
            }

            if (buf.readableBytes() >= 2 * 4) {

                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);

                position.set(Position.KEY_RSSI, buf.readUnsignedShort());

                position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
                position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());

            }

        } else if (type == MSG_ALARM) {

            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));

        } else if (type == MSG_STATE) {

            int statusType = buf.readUnsignedByte();

            position.set(Position.KEY_EVENT, statusType);

            if (statusType == 0x01 || statusType == 0x02 || statusType == 0x03) {
                buf.readUnsignedInt(); // device time
                if (buf.readableBytes() >= 2) {
                    decodeStatus(position, buf.readUnsignedShort());
                }
            }

        }

        return position;
    }

    private Position decodeNew(DeviceSession deviceSession, ByteBuf buf, int type, int index) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, index);

        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        int flags = buf.readUnsignedByte();

        if (BitUtil.check(flags, 0)) {
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        } else {
            getLastLocation(position, position.getDeviceTime());
        }

        Network network = new Network();

        int mcc = 0;
        int mnc = 0;
        if (BitUtil.check(flags, 1)) {
            mcc = buf.readUnsignedShort();
            mnc = buf.readUnsignedShort();
            network.addCellTower(CellTower.from(
                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 2)) {
            network.addCellTower(CellTower.from(
                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 3)) {
            network.addCellTower(CellTower.from(
                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt(), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 4)) {
            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
            network.addWifiAccessPoint(WifiAccessPoint.from(
                    mac.substring(0, mac.length() - 1), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 5)) {
            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
            network.addWifiAccessPoint(WifiAccessPoint.from(
                    mac.substring(0, mac.length() - 1), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 6)) {
            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
            network.addWifiAccessPoint(WifiAccessPoint.from(
                    mac.substring(0, mac.length() - 1), buf.readUnsignedByte()));
        }

        if (BitUtil.check(flags, 7)) {
            buf.readUnsignedByte(); // radio access technology
            int count = buf.readUnsignedByte();
            int lac = 0;
            if (count > 0) {
                mcc = buf.readUnsignedShort();
                mnc = buf.readUnsignedShort();
                lac = buf.readUnsignedShort(); // lac
                buf.readUnsignedShort(); // tac
                buf.readUnsignedInt(); // cid
                buf.readUnsignedShort(); // ta
            }
            for (int i = 0; i < count; i++) {
                int cid = buf.readUnsignedShort(); // physical cid
                buf.readUnsignedShort(); // e-arfcn
                int rssi = buf.readUnsignedByte();
                network.addCellTower(CellTower.from(mcc, mnc, lac, cid, rssi));
            }
        }

        if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
            position.setNetwork(network);
        }

        if (type == MSG_WARNING) {

            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));

        } else if (type == MSG_REPORT) {

            buf.readUnsignedByte(); // report type

        }

        if (type == MSG_NORMAL || type == MSG_WARNING || type == MSG_REPORT) {

            int status = buf.readUnsignedShort();
            position.setValid(BitUtil.check(status, 0));
            if (BitUtil.check(status, 1)) {
                position.set(Position.KEY_IGNITION, BitUtil.check(status, 2));
            }
            position.set(Position.KEY_STATUS, status);

        }

        if (type == MSG_NORMAL) {

            if (buf.readableBytes() >= 2) {
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
            }

            if (buf.readableBytes() >= 4) {
                position.set(Position.PREFIX_ADC + 0, buf.readUnsignedShort());
                position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
            }

            if (buf.readableBytes() >= 4) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (buf.readableBytes() >= 4) {
                buf.readUnsignedShort(); // gsm counter
                buf.readUnsignedShort(); // gps counter
            }

            if (buf.readableBytes() >= 4) {
                position.set(Position.KEY_STEPS, buf.readUnsignedShort());
                buf.readUnsignedShort(); // walking time
            }

            if (buf.readableBytes() >= 12) {
                position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort() / 256.0);
                position.set("humidity", buf.readUnsignedShort() * 0.1);
                position.set("illuminance", buf.readUnsignedInt() / 256.0);
                position.set("co2", buf.readUnsignedInt());
            }

            if (buf.readableBytes() >= 2) {
                position.set(Position.PREFIX_TEMP + 2, buf.readShort() / 16.0);
            }

            if (buf.readableBytes() >= 2) {
                int count = buf.readUnsignedByte();
                buf.readUnsignedByte(); // id
                for (int i = 1; i <= count; i++) {
                    position.set("tag" + i + "Id", ByteBufUtil.hexDump(buf.readSlice(6)));
                    buf.readUnsignedByte(); // signal level
                    buf.readUnsignedByte(); // reserved
                    buf.readUnsignedByte(); // model
                    buf.readUnsignedByte(); // version
                    position.set("tag" + i + "Battery", buf.readUnsignedShort() * 0.001);
                    position.set("tag" + i + "Temp", buf.readShort() / 256.0);
                    position.set("tag" + i + "Data", buf.readUnsignedShort());
                }

            }

        }

        return position;
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("Lat:")
            .number("([NS])(d+.d+)")             // latitude
            .any()
            .text("Lon:")
            .number("([EW])(d+.d+)")             // longitude
            .any()
            .text("Course:")
            .number("(d+.d+)")                   // course
            .any()
            .text("Speed:")
            .number("(d+.d+)")                   // speed
            .any()
            .expression("Date ?Time:")
            .number("(dddd)-(dd)-(dd) ")         // date
            .number("(dd):(dd):(dd)")            // time
            .compile();

    private Position decodeResult(DeviceSession deviceSession, ByteBuf buf, int index) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_INDEX, index);

        buf.readUnsignedByte(); // type
        buf.readUnsignedInt(); // uid

        String sentence = buf.toString(StandardCharsets.UTF_8);

        Parser parser = new Parser(PATTERN, sentence);
        if (parser.matches()) {

            position.setValid(true);
            position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
            position.setCourse(parser.nextDouble());
            position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));
            position.setTime(parser.nextDateTime());

        } else {

            getLastLocation(position, null);

            position.set(Position.KEY_RESULT, sentence);

        }

        return position;
    }

    private Position decodeObd(DeviceSession deviceSession, ByteBuf buf) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, new Date(buf.readUnsignedInt() * 1000));

        while (buf.readableBytes() > 0) {
            int pid = buf.readUnsignedByte();
            int value = buf.readInt();
            switch (pid) {
                case 0x89:
                    position.set(Position.KEY_FUEL_CONSUMPTION, value);
                    break;
                case 0x8a:
                    position.set(Position.KEY_ODOMETER, value * 1000L);
                    break;
                case 0x8b:
                    position.set(Position.KEY_FUEL_LEVEL, value / 10);
                    break;
                default:
                    break;
            }
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        String uniqueId = null;
        DeviceSession deviceSession;

        if (buf.getByte(0) == 'E' && buf.getByte(1) == 'L') {
            buf.skipBytes(2 + 2 + 2); // udp header
            uniqueId = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            deviceSession = getDeviceSession(channel, remoteAddress, uniqueId);
        } else {
            deviceSession = getDeviceSession(channel, remoteAddress);
        }

        List<Position> positions = new LinkedList<>();

        while (buf.isReadable()) {
            Position position = decodePackage(channel, remoteAddress, buf, uniqueId, deviceSession);
            if (position != null) {
                positions.add(position);
            }
        }

        if (!positions.isEmpty()) {
            return positions.size() > 1 ? positions : positions.iterator().next();
        } else {
            return null;
        }
    }

    protected Position decodePackage(
            Channel channel, SocketAddress remoteAddress, ByteBuf buf,
            String uniqueId, DeviceSession deviceSession) throws Exception {

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf = buf.readSlice(buf.readUnsignedShort());
        int index = buf.readUnsignedShort();

        if (type != MSG_GPS && type != MSG_DATA) {
            ByteBuf content = Unpooled.buffer();
            if (type == MSG_LOGIN) {
                content.writeInt((int) (System.currentTimeMillis() / 1000));
                content.writeShort(1); // protocol version
                content.writeByte(0); // action mask
            }
            ByteBuf response = EelinkProtocolEncoder.encodeContent(
                    channel instanceof DatagramChannel, uniqueId, type, index, content);
            content.release();
            if (channel != null) {
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }
        }

        if (type == MSG_LOGIN) {

            if (deviceSession == null) {
                getDeviceSession(channel, remoteAddress, ByteBufUtil.hexDump(buf.readSlice(8)).substring(1));
            }

        } else {

            if (deviceSession == null) {
                return null;
            }

            if (type == MSG_GPS || type == MSG_ALARM || type == MSG_STATE || type == MSG_SMS) {

                return decodeOld(deviceSession, buf, type, index);

            } else if (type >= MSG_NORMAL && type <= MSG_OBD_CODE) {

                return decodeNew(deviceSession, buf, type, index);

            } else if (type == MSG_HEARTBEAT && buf.readableBytes() >= 2
                    || type == MSG_OBD && buf.readableBytes() == 4) {

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                getLastLocation(position, null);

                decodeStatus(position, buf.readUnsignedShort());

                return position;

            } else if (type == MSG_OBD) {

                return decodeObd(deviceSession, buf);

            } else if (type == MSG_DOWNLINK) {

                return decodeResult(deviceSession, buf, index);

            }

        }

        return null;
    }

}
