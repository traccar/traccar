/*
 * Copyright 2013 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.jboss.netty.channel.socket.DatagramChannel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TeltonikaProtocolDecoder extends BaseProtocolDecoder {

    public TeltonikaProtocolDecoder(TeltonikaProtocol protocol) {
        super(protocol);
    }

    private DeviceSession parseIdentification(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) {

        int length = buf.readUnsignedShort();
        String imei = buf.toString(buf.readerIndex(), length, StandardCharsets.US_ASCII);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(1);
            if (deviceSession != null) {
                response.writeByte(1);
            } else {
                response.writeByte(0);
            }
            channel.write(response);
        }
        return deviceSession;
    }

    public static final int CODEC_GH3000 = 0x07;
    public static final int CODEC_FM4X00 = 0x08;
    public static final int CODEC_12 = 0x0C;

    private void decodeSerial(Position position, ChannelBuffer buf) {

        getLastLocation(position, null);

        position.set(Position.KEY_TYPE, buf.readUnsignedByte());

        position.set(Position.KEY_COMMAND, buf.readBytes(buf.readInt()).toString(StandardCharsets.US_ASCII));

    }

    private long readValue(ChannelBuffer buf, int length, boolean signed) {
        switch (length) {
            case 1:
                return signed ? buf.readByte() : buf.readUnsignedByte();
            case 2:
                return signed ? buf.readShort() : buf.readUnsignedShort();
            case 4:
                return signed ? buf.readInt() : buf.readUnsignedInt();
            default:
                return buf.readLong();
        }
    }

    private void decodeParameter(Position position, int id, ChannelBuffer buf, int length) {
        switch (id) {
            case 1:
            case 2:
            case 3:
            case 4:
                position.set("di" + id, readValue(buf, length, false));
                break;
            case 9:
                position.set(Position.PREFIX_ADC + 1, readValue(buf, length, false));
                break;
            case 66:
                position.set(Position.KEY_POWER, readValue(buf, length, false) * 0.001);
                break;
            case 67:
                position.set(Position.KEY_BATTERY, readValue(buf, length, false) * 0.001);
                break;
            case 70:
                position.set(Position.KEY_DEVICE_TEMP, readValue(buf, length, true) * 0.1);
                break;
            case 72:
                position.set(Position.PREFIX_TEMP + 1, readValue(buf, length, true) * 0.1);
                break;
            case 73:
                position.set(Position.PREFIX_TEMP + 2, readValue(buf, length, true) * 0.1);
                break;
            case 74:
                position.set(Position.PREFIX_TEMP + 3, readValue(buf, length, true) * 0.1);
                break;
            case 78:
                position.set(Position.KEY_RFID, readValue(buf, length, false));
                break;
            case 182:
                position.set(Position.KEY_HDOP, readValue(buf, length, false) * 0.1);
                break;
            default:
                position.set(Position.PREFIX_IO + id, readValue(buf, length, false));
                break;
        }
    }

    private void decodeLocation(Position position, ChannelBuffer buf, int codec) {

        int globalMask = 0x0f;

        if (codec == CODEC_GH3000) {

            long time = buf.readUnsignedInt() & 0x3fffffff;
            time += 1167609600; // 2007-01-01 00:00:00

            globalMask = buf.readUnsignedByte();
            if (BitUtil.check(globalMask, 0)) {

                position.setTime(new Date(time * 1000));

                int locationMask = buf.readUnsignedByte();

                if (BitUtil.check(locationMask, 0)) {
                    position.setLatitude(buf.readFloat());
                    position.setLongitude(buf.readFloat());
                }

                if (BitUtil.check(locationMask, 1)) {
                    position.setAltitude(buf.readUnsignedShort());
                }

                if (BitUtil.check(locationMask, 2)) {
                    position.setCourse(buf.readUnsignedByte() * 360.0 / 256);
                }

                if (BitUtil.check(locationMask, 3)) {
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
                }

                if (BitUtil.check(locationMask, 4)) {
                    int satellites = buf.readUnsignedByte();
                    position.set(Position.KEY_SATELLITES, satellites);
                    position.setValid(satellites >= 3);
                }

                if (BitUtil.check(locationMask, 5)) {
                    CellTower cellTower = CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort());

                    if (BitUtil.check(locationMask, 6)) {
                        cellTower.setSignalStrength((int) buf.readUnsignedByte());
                    }

                    position.setNetwork(new Network(cellTower));

                } else if (BitUtil.check(locationMask, 6)) {
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                }

                if (BitUtil.check(locationMask, 7)) {
                    position.set(Position.KEY_OPERATOR, buf.readUnsignedInt());
                }

            } else {

                getLastLocation(position, new Date(time * 1000));

            }

        } else {

            position.setTime(new Date(buf.readLong()));

            position.set("priority", buf.readUnsignedByte());

            position.setLongitude(buf.readInt() / 10000000.0);
            position.setLatitude(buf.readInt() / 10000000.0);
            position.setAltitude(buf.readShort());
            position.setCourse(buf.readUnsignedShort());

            int satellites = buf.readUnsignedByte();
            position.set(Position.KEY_SATELLITES, satellites);

            position.setValid(satellites != 0);

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.set(Position.KEY_EVENT, buf.readUnsignedByte());

            buf.readUnsignedByte(); // total IO data records

        }

        // Read 1 byte data
        if (BitUtil.check(globalMask, 1)) {
            int cnt = buf.readUnsignedByte();
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, buf.readUnsignedByte(), buf, 1);
            }
        }

        // Read 2 byte data
        if (BitUtil.check(globalMask, 2)) {
            int cnt = buf.readUnsignedByte();
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, buf.readUnsignedByte(), buf, 2);
            }
        }

        // Read 4 byte data
        if (BitUtil.check(globalMask, 3)) {
            int cnt = buf.readUnsignedByte();
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, buf.readUnsignedByte(), buf, 4);
            }
        }

        // Read 8 byte data
        if (codec == CODEC_FM4X00) {
            int cnt = buf.readUnsignedByte();
            for (int j = 0; j < cnt; j++) {
                decodeParameter(position, buf.readUnsignedByte(), buf, 8);
            }
        }

    }

    private List<Position> parseData(
            Channel channel, SocketAddress remoteAddress, ChannelBuffer buf, int packetId, String... imei) {
        List<Position> positions = new LinkedList<>();

        if (!(channel instanceof DatagramChannel)) {
            buf.readUnsignedInt(); // data length
        }

        int codec = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (deviceSession == null) {
            return null;
        }

        for (int i = 0; i < count; i++) {
            Position position = new Position();
            position.setProtocol(getProtocolName());

            position.setDeviceId(deviceSession.getDeviceId());

            if (codec == CODEC_12) {
                decodeSerial(position, buf);
            } else {
                decodeLocation(position, buf, codec);
            }

            positions.add(position);
        }

        if (channel != null) {
            if (channel instanceof DatagramChannel) {
                ChannelBuffer response = ChannelBuffers.directBuffer(5);
                response.writeShort(3);
                response.writeShort(packetId);
                response.writeByte(0x02);
                channel.write(response, remoteAddress);
            } else {
                ChannelBuffer response = ChannelBuffers.directBuffer(4);
                response.writeInt(count);
                channel.write(response);
            }
        }

        return positions;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (channel instanceof DatagramChannel) {
            return decodeUdp(channel, remoteAddress, buf);
        } else {
            return decodeTcp(channel, remoteAddress, buf);
        }
    }

    private Object decodeTcp(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) throws Exception {

        if (buf.getUnsignedShort(0) > 0) {
            parseIdentification(channel, remoteAddress, buf);
        } else {
            buf.skipBytes(4);
            return parseData(channel, remoteAddress, buf, 0);
        }

        return null;
    }

    private Object decodeUdp(Channel channel, SocketAddress remoteAddress, ChannelBuffer buf) throws Exception {

        buf.skipBytes(2);
        int packetId = buf.readUnsignedShort();
        buf.skipBytes(2);
        String imei = buf.readBytes(buf.readUnsignedShort()).toString(StandardCharsets.US_ASCII);

        return parseData(channel, remoteAddress, buf, packetId, imei);

    }

}
