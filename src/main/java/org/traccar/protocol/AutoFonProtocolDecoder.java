/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2015 Vitaly Litvak (vitavaque@gmail.com)
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
import org.traccar.helper.DateBuilder;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class AutoFonProtocolDecoder extends BaseProtocolDecoder {

    public AutoFonProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x10;
    public static final int MSG_LOCATION = 0x11;
    public static final int MSG_HISTORY = 0x12;

    public static final int MSG_45_LOGIN = 0x41;
    public static final int MSG_45_LOCATION = 0x02;

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    private static double convertCoordinate(short degrees, int minutes) {
        double value = degrees + BitUtil.from(minutes, 4) / 600000.0;
        if (BitUtil.check(minutes, 0)) {
            return value;
        } else {
            return -value;
        }
    }

    private Position decodePosition(DeviceSession deviceSession, ByteBuf buf, boolean history) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (!history) {
            buf.readUnsignedByte(); // interval
            buf.skipBytes(8); // settings
        }
        position.set(Position.KEY_STATUS, buf.readUnsignedByte());
        if (!history) {
            buf.readUnsignedShort();
        }
        position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
        buf.skipBytes(6); // time

        if (!history) {
            for (int i = 0; i < 2; i++) {
                buf.skipBytes(5); // time
                buf.readUnsignedShort(); // interval
                buf.skipBytes(5); // mode
            }
        }

        position.set(Position.PREFIX_TEMP + 1, buf.readByte());

        int rssi = buf.readUnsignedByte();
        CellTower cellTower = CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedShort(),
                buf.readUnsignedShort(), buf.readUnsignedShort(), rssi);
        position.setNetwork(new Network(cellTower));

        int valid = buf.readUnsignedByte();
        position.setValid((valid & 0xc0) != 0);
        position.set(Position.KEY_SATELLITES, valid & 0x3f);

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(convertCoordinate(buf.readInt()));
        position.setLongitude(convertCoordinate(buf.readInt()));
        position.setAltitude(buf.readShort());
        position.setSpeed(buf.readUnsignedByte());
        position.setCourse(buf.readUnsignedByte() * 2.0);

        position.set(Position.KEY_HDOP, buf.readUnsignedShort());

        buf.readUnsignedShort(); // reserved
        buf.readUnsignedByte(); // checksum
        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int type = buf.readUnsignedByte();

        if (type == MSG_LOGIN || type == MSG_45_LOGIN) {

            if (type == MSG_LOGIN) {
                buf.readUnsignedByte(); // hardware version
                buf.readUnsignedByte(); // software version
            }

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

            if (deviceSession != null && channel != null) {
                ByteBuf response = Unpooled.buffer();
                response.writeBytes("resp_crc=".getBytes(StandardCharsets.US_ASCII));
                response.writeByte(buf.getByte(buf.writerIndex() - 1));
                channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
            }

            return null;

        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (type == MSG_LOCATION) {

            return decodePosition(deviceSession, buf, false);

        } else if (type == MSG_HISTORY) {

            int count = buf.readUnsignedByte() & 0x0f;
            buf.readUnsignedShort(); // total count
            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                positions.add(decodePosition(deviceSession, buf, true));
            }

            return positions;

        } else if (type == MSG_45_LOCATION) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            short status = buf.readUnsignedByte();
            if (BitUtil.check(status, 7)) {
                position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
            }
            position.set(Position.KEY_BATTERY, BitUtil.to(status, 7));

            buf.skipBytes(2); // remaining time

            position.set(Position.PREFIX_TEMP + 1, buf.readByte());

            buf.skipBytes(2); // timer (interval and units)
            buf.readByte(); // mode
            buf.readByte(); // gprs sending interval

            buf.skipBytes(6); // mcc, mnc, lac, cid

            int valid = buf.readUnsignedByte();
            position.setValid(BitUtil.from(valid, 6) != 0);
            position.set(Position.KEY_SATELLITES, BitUtil.from(valid, 6));

            int time = buf.readUnsignedMedium();
            int date = buf.readUnsignedMedium();

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(time / 10000, time / 100 % 100, time % 100)
                    .setDateReverse(date / 10000, date / 100 % 100, date % 100);
            position.setTime(dateBuilder.getDate());

            position.setLatitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedMedium()));
            position.setLongitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedMedium()));
            position.setSpeed(buf.readUnsignedByte());
            position.setCourse(buf.readUnsignedShort());

            return position;

        }

        return null;
    }

}
