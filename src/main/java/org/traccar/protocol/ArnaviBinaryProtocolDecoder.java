/*
 * Copyright 2020 - 2026 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Ivan Muratov (binakot@gmail.com)
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
import org.traccar.helper.BitUtil;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ArnaviBinaryProtocolDecoder extends BaseProtocolDecoder {

    private static final int HEADER_START_SIGN = 0xff;

    private static final int HEADER_VERSION_1 = 0x22;
    private static final int HEADER_VERSION_2 = 0x23;
    private static final int HEADER_VERSION_3 = 0x24;

    private static final int RECORD_PING = 0x00;
    private static final int RECORD_DATA = 0x01;
    private static final int RECORD_TEXT = 0x03;
    private static final int RECORD_FILE = 0x04;
    private static final int RECORD_BINARY = 0x06;

    public ArnaviBinaryProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, int version, int index) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x7b);

            if (version == HEADER_VERSION_1) {
                response.writeByte(0x00);
                response.writeByte((byte) index);
            } else if (version == HEADER_VERSION_2) {
                response.writeByte(0x04);
                response.writeByte(0x00);
                ByteBuf timeBytes = Unpooled.buffer(4);
                timeBytes.writeIntLE((int) (System.currentTimeMillis() / 1000));
                response.writeByte(Checksum.modulo256(timeBytes.nioBuffer()));
                response.writeBytes(timeBytes);
                timeBytes.release();
            }
            response.writeByte(0x7d);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private Position decodePosition(DeviceSession deviceSession, ByteBuf buf, int length, Date time) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(time);

        int cid = 0, lac = 0, mnc = 0, mcc = 0, rssi = 0;

        int endIndex = buf.readerIndex() + length;
        while (buf.readerIndex() < endIndex) {
            int tag = buf.readUnsignedByte();
            switch (tag) {

                case 1:
                    int battery = buf.readUnsignedShortLE();
                    if (battery != 0x0000 && battery != 0xffff) {
                        position.set(Position.KEY_BATTERY, battery / 1000.0);
                    }
                    int power = buf.readUnsignedShortLE();
                    if (power != 0x0000 && power != 0xffff) {
                        position.set(Position.KEY_POWER, power / 1000.0);
                    }
                    break;

                case 3:
                    position.setLatitude(buf.readFloatLE());
                    break;

                case 4:
                    position.setLongitude(buf.readFloatLE());
                    break;

                case 5:
                    position.setCourse(buf.readUnsignedByte() * 2);
                    position.setAltitude(buf.readUnsignedByte() * 10);
                    int satellites = buf.readUnsignedByte();
                    position.setValid(satellites != 0xfe && satellites != 0xff);
                    position.set(Position.KEY_SATELLITES,
                            BitUtil.between(satellites, 0, 4) + BitUtil.between(satellites, 4, 8));
                    position.setSpeed(buf.readUnsignedByte());
                    break;

                case 6:
                    switch (buf.readUnsignedByte()) {
                        case 0x01 -> {
                            int flags = buf.readUnsignedByte();
                            position.set(Position.KEY_IGNITION, BitUtil.check(flags, 0));
                            position.set("callButton", BitUtil.check(flags, 1));
                            position.set(Position.KEY_INPUT, buf.readUnsignedShortLE());
                        }
                        case 0x06 -> position.set("pulses" + buf.readUnsignedByte(), buf.readUnsignedShortLE());
                        case 0x07 -> position.set("freq" + buf.readUnsignedByte(), buf.readUnsignedShortLE());
                        case 0x08 -> position.set(
                                Position.PREFIX_ADC + buf.readUnsignedByte(), buf.readUnsignedShortLE() / 1000.0);
                        default -> buf.skipBytes(3);
                    }
                    break;

                case 7:
                    cid = buf.readUnsignedShortLE();
                    lac = buf.readUnsignedShortLE();
                    break;

                case 8:
                    rssi = buf.readUnsignedByte();
                    mcc = buf.readUnsignedShortLE();
                    mnc = buf.readUnsignedByte();
                    break;

                case 9:
                    position.set(Position.KEY_STATUS, buf.readUnsignedIntLE());
                    break;

                case 51:
                    position.set("canStatus", buf.readUnsignedIntLE());
                    break;

                case 52:
                    position.set(Position.KEY_HOURS, buf.readUnsignedIntLE() * 3600000 / 100);
                    break;

                case 53:
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE() * 10);
                    break;

                case 54:
                    position.set(Position.KEY_FUEL_USED, buf.readUnsignedIntLE() / 10.0);
                    break;

                case 55:
                    position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShortLE() / 10.0);
                    buf.skipBytes(2);
                    break;

                case 56:
                    position.set(Position.KEY_FUEL, buf.readUnsignedIntLE());
                    break;

                case 57:
                    position.set(Position.KEY_RPM, buf.readUnsignedShortLE());
                    buf.skipBytes(2);
                    break;

                case 58:
                    position.set(Position.KEY_ENGINE_TEMP, buf.readUnsignedIntLE());
                    break;

                case 59:
                    position.set(Position.KEY_OBD_SPEED, buf.readUnsignedIntLE());
                    break;

                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                    position.set("axleLoad" + (tag - 59), buf.readUnsignedIntLE());
                    break;

                case 69:
                    position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte());
                    position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                    int motionStatus = buf.readUnsignedShortLE();
                    if (motionStatus != 0) {
                        position.set("motionStatus", motionStatus);
                    }
                    break;

                case 71:
                case 72:
                case 73:
                case 74:
                case 75:
                case 76:
                case 77:
                case 78:
                case 79:
                    int level = buf.readUnsignedShortLE();
                    if (level != 0x0000 && level != 0xffff) {
                        position.set("llsLevel" + (tag - 70), level);
                        position.set("llsTemp" + (tag - 70), (buf.readUnsignedShortLE() - 100.0) / 10.0);
                    }
                    break;

                case 151:
                    position.set(Position.KEY_HDOP, buf.readUnsignedShortLE() / 100.0);
                    buf.skipBytes(2);
                    break;

                default:
                    buf.skipBytes(4);
                    break;
            }
        }

        if (mcc != 0 || mnc != 0 || lac != 0 || cid != 0 || rssi != 0) {
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid, rssi)));
        }

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readUnsignedByte();

        if (header == HEADER_START_SIGN) {

            int version = buf.readUnsignedByte();
            String imei = String.valueOf(buf.readLongLE());
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                sendResponse(channel, version, 0);
            }

            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        int index = buf.readUnsignedByte();

        while (buf.readableBytes() > 1) {

            int type = buf.readUnsignedByte();

            switch (type) {
                case RECORD_PING, RECORD_DATA, RECORD_TEXT, RECORD_FILE, RECORD_BINARY -> {
                    int length = buf.readUnsignedShortLE();
                    Date time = new Date(buf.readUnsignedIntLE() * 1000);
                    if (type == RECORD_DATA) {
                        positions.add(decodePosition(deviceSession, buf, length, time));
                    } else {
                        buf.skipBytes(length);
                    }
                    buf.readUnsignedByte();
                }

                default -> {
                    return positions.isEmpty() ? null : positions;
                }
            }
        }

        sendResponse(channel, HEADER_VERSION_1, index);

        return positions.isEmpty() ? null : positions;
    }

}
