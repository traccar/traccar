/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Xexun2ProtocolDecoder extends BaseProtocolDecoder {

    public Xexun2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_POSITION = 0x14;

    private void sendResponse(Channel channel, int type, int index, ByteBuf imei) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xfa);
            response.writeByte(0xaf);

            response.writeShort(type);
            response.writeShort(index);
            response.writeBytes(imei);
            response.writeShort(1); // attributes / length
            response.writeShort(0xfffe); // checksum
            response.writeByte(1); // response

            response.writeByte(0xfa);
            response.writeByte(0xaf);

            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm(long value) {
        if (BitUtil.check(value, 0)) {
            return Position.ALARM_SOS;
        }
        if (BitUtil.check(value, 15)) {
            return Position.ALARM_FALL_DOWN;
        }
        return null;
    }

    private double convertCoordinate(double value) {
        double degrees = Math.floor(value / 100);
        double minutes = value - degrees * 100;
        return degrees + minutes / 60;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // flag
        int type = buf.readUnsignedShort();
        int index = buf.readUnsignedShort();

        ByteBuf imei = buf.readSlice(8);
        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, ByteBufUtil.hexDump(imei).substring(0, 15));
        if (deviceSession == null) {
            return null;
        }

        sendResponse(channel, type, index, imei);

        buf.readUnsignedShort(); // attributes
        buf.readUnsignedShort(); // checksum

        if (type == MSG_POSITION) {
            List<Integer> lengths = new ArrayList<>();
            List<Position> positions = new ArrayList<>();

            int count = buf.readUnsignedByte();
            for (int i = 0; i < count; i++) {
                lengths.add(buf.readUnsignedShort());
            }

            for (int i = 0; i < count; i++) {
                int endIndex = buf.readerIndex() + lengths.get(i);

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.set(Position.KEY_INDEX, buf.readUnsignedByte());

                position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));

                position.set(Position.KEY_RSSI, buf.readUnsignedByte());

                int battery = buf.readUnsignedShort();
                position.set(Position.KEY_CHARGE, BitUtil.check(battery, 15));
                position.set(Position.KEY_BATTERY_LEVEL, BitUtil.to(battery, 15));

                int mask = buf.readUnsignedByte();

                if (BitUtil.check(mask, 0)) {
                    position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedInt()));
                }
                if (BitUtil.check(mask, 1)) {
                    int positionMask = buf.readUnsignedByte();
                    if (BitUtil.check(positionMask, 0)) {
                        position.setValid(true);
                        position.setFixTime(position.getDeviceTime());
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        position.setLongitude(convertCoordinate(buf.readFloat()));
                        position.setLatitude(convertCoordinate(buf.readFloat()));
                    }
                    Network network = new Network();
                    if (BitUtil.check(positionMask, 1)) {
                        int wifiCount = buf.readUnsignedByte();
                        for (int j = 0; j < wifiCount; j++) {
                            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                            network.addWifiAccessPoint(WifiAccessPoint.from(
                                    mac.substring(0, mac.length() - 1), buf.readUnsignedByte()));
                        }
                    }
                    if (BitUtil.check(positionMask, 2)) {
                        int cellCount = buf.readUnsignedByte();
                        for (int j = 0; j < cellCount; j++) {
                            network.addCellTower(CellTower.from(
                                    buf.readUnsignedShort(), buf.readUnsignedShort(),
                                    buf.readInt(), buf.readUnsignedInt(), buf.readUnsignedByte()));
                        }
                    }
                    if (network.getWifiAccessPoints() != null || network.getCellTowers() != null) {
                        position.setNetwork(network);
                    }
                    if (BitUtil.check(positionMask, 3)) {
                        buf.skipBytes(12 * buf.readUnsignedByte()); // tof
                    }
                    if (BitUtil.check(positionMask, 5)) {
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
                        position.setCourse(buf.readUnsignedShort() * 0.1);
                    }
                    if (BitUtil.check(positionMask, 6)) {
                        position.setValid(true);
                        position.setFixTime(position.getDeviceTime());
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        position.setLongitude(convertCoordinate(buf.readDouble()));
                        position.setLatitude(convertCoordinate(buf.readDouble()));

                    }
                }
                if (BitUtil.check(mask, 3)) {
                    buf.readUnsignedInt(); // fingerprint
                }
                if (BitUtil.check(mask, 4)) {
                    buf.skipBytes(20); // version
                    buf.skipBytes(8); // imsi
                    buf.skipBytes(10); // iccid
                }
                if (BitUtil.check(mask, 5)) {
                    buf.skipBytes(12); // device parameters
                }

                if (!position.getValid()) {
                    getLastLocation(position, position.getDeviceTime());
                }
                positions.add(position);

                buf.readerIndex(endIndex);
            }

            return positions;
        }

        return null;
    }

}
