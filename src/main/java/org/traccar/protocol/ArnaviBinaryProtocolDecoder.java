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

    private static final int HEADER_START_SIGN = 0xFF;

    private static final int HEADER_VERSION_1 = 0x22;
    private static final int HEADER_VERSION_2 = 0x23;
    private static final int HEADER_VERSION_3 = 0x24;
    // private static final int HEADER_VERSION_4 = 0x25;

    private static final int RECORD_PING = 0x00;
    private static final int RECORD_DATA = 0x01;
    private static final int RECORD_TEXT = 0x03;
    private static final int RECORD_FILE = 0x04;
    private static final int RECORD_BINARY = 0x06;

    public ArnaviBinaryProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, int version, int packageNumber) {
        if (channel == null) {
            return;
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x7B);

        if (packageNumber == 0) {
            if (version == HEADER_VERSION_1) {
                response.writeByte(0x00);
                response.writeByte(0x00);
            } else {
                response.writeByte(0x04);
                response.writeByte(0x00);

                long unixTime = System.currentTimeMillis() / 1000;
                ByteBuf timeBytes = Unpooled.buffer(4);
                timeBytes.writeIntLE((int) unixTime);

                response.writeByte(Checksum.modulo256(timeBytes.nioBuffer()));
                response.writeBytes(timeBytes);
                timeBytes.release();
            }
        } else {
            response.writeByte(0x00);
            response.writeByte((byte) packageNumber);
        }

        response.writeByte(0x7D);
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private Position decodePosition(DeviceSession deviceSession, ByteBuf buf, int length, Date time) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(time);

        int cid = 0, lac = 0, mnc = 0, mcc = 0, rssi = 0;

        int readBytes = 0;
        while (readBytes < length) {

            int tag = buf.readUnsignedByte();

            switch (tag) {

                case 1: {
                    int batMv = buf.readUnsignedShortLE();
                    int extMv = buf.readUnsignedShortLE();
                    if (extMv != 0x0000 && extMv != 0xFFFF) {
                        position.set(Position.KEY_POWER, extMv / 1000.0);
                    }
                    if (batMv != 0x0000 && batMv != 0xFFFF) {
                        position.set(Position.KEY_BATTERY, batMv / 1000.0);
                    }
                    break;
                }

                case 3: {
                    position.setLatitude(buf.readFloatLE());
                    break;
                }

                case 4: {
                    position.setLongitude(buf.readFloatLE());
                    break;
                }

                case 5: {
                    int course = buf.readUnsignedByte() * 2;
                    int altitude = buf.readUnsignedByte() * 10;
                    int satByte = buf.readUnsignedByte();
                    int speed = buf.readUnsignedByte();
                    if (satByte == 0xFE || satByte == 0xFF) {
                        position.setValid(false);
                    } else {
                        position.setCourse(course);
                        position.setAltitude(altitude);
                        position.setSpeed(speed);
                        position.set(Position.KEY_SATELLITES,
                                BitUtil.between(satByte, 0, 4) + BitUtil.between(satByte, 4, 8));
                        position.setValid(true);
                    }
                    break;
                }

                case 6: {
                    switch (buf.readUnsignedByte()) {
                        case 0x01: {
                            position.set("virtualIgnition", BitUtil.check(buf.readUnsignedByte(), 0));
                            position.set("callButton", BitUtil.check(buf.readUnsignedByte(), 1));
                            for (int i = 0; i < 8; i++) {
                                position.set(Position.PREFIX_IN + (i + 1), BitUtil.check(buf.readUnsignedShortLE(), i));
                            }
                            break;
                        }
                        case 0x06: {
                            position.set("pulses" + buf.readUnsignedByte(), buf.readUnsignedShortLE());
                            break;
                        }
                        case 0x07: {
                            position.set("freq" + buf.readUnsignedByte(), buf.readUnsignedShortLE());
                            break;
                        }
                        case 0x08: {
                            position.set(Position.PREFIX_ADC + buf.readUnsignedByte(),
                                    buf.readUnsignedShortLE() / 1000.0);
                            break;
                        }
                        default:
                            buf.skipBytes(3);
                            break;
                    }
                    break;
                }

                case 7: {
                    cid = buf.readUnsignedShortLE();
                    lac = buf.readUnsignedShortLE();
                    break;
                }

                case 8: {
                    rssi = buf.readUnsignedByte();
                    mcc = buf.readUnsignedShortLE();
                    mnc = buf.readUnsignedByte();
                    break;
                }

                case 9: {
                    long status = buf.readUnsignedIntLE();
                    position.set(Position.KEY_STATUS, status);
                    position.set(Position.KEY_OUTPUT, BitUtil.between(status, 8, 12));
                    position.set("gsmState", BitUtil.between(status, 12, 14));
                    position.set("gpsState", BitUtil.between(status, 14, 16));
                    position.set(Position.KEY_MOTION, BitUtil.check(status, 16));
                    position.set("simPresent", BitUtil.check(status, 18));
                    position.set("guardMode", BitUtil.check(status, 19));
                    position.set("alarm", BitUtil.check(status, 20) ? Position.ALARM_SOS : null);
                    break;
                }

                case 51: {
                    long flags = buf.readUnsignedIntLE();
                    position.set("doorDriver", BitUtil.check(flags, 8));
                    position.set("doorPassenger", BitUtil.check(flags, 9));
                    position.set("trunkOpen", BitUtil.check(flags, 10));
                    position.set("hoodOpen", BitUtil.check(flags, 11));
                    position.set("handbrake", BitUtil.check(flags, 12));
                    position.set("brakePedal", BitUtil.check(flags, 13));
                    position.set("engineRunning", BitUtil.check(flags, 14));
                    position.set("canIgnition", BitUtil.check(flags, 16));
                    position.set("keyInIgnition", BitUtil.check(flags, 19));
                    break;
                }

                case 52: {
                    position.set(Position.KEY_HOURS, (long) (buf.readUnsignedIntLE() / 100.0 * 3600 * 1000));
                    break;
                }

                case 53: {
                    position.set(Position.KEY_ODOMETER, (long) (buf.readUnsignedIntLE() / 100.0 * 1000));
                    break;
                }

                case 54: {
                    position.set(Position.KEY_FUEL_USED, buf.readUnsignedIntLE() / 10.0);
                    break;
                }

                case 55: {
                    position.set(Position.KEY_FUEL_LEVEL, buf.readUnsignedShortLE() / 10.0);
                    buf.skipBytes(2);
                    break;
                }

                case 56: {
                    position.set("fuelLitres", buf.readUnsignedIntLE());
                    break;
                }

                case 57: {
                    position.set(Position.KEY_RPM, buf.readUnsignedShortLE());
                    buf.skipBytes(2);
                    break;
                }

                case 58: {
                    position.set(Position.KEY_ENGINE_TEMP, buf.readUnsignedIntLE());
                    break;
                }

                case 59: {
                    position.set(Position.KEY_OBD_SPEED, buf.readUnsignedIntLE());
                    break;
                }

                case 60:
                case 61:
                case 62:
                case 63:
                case 64: {
                    int axleIndex = tag - 60 + 1;
                    if (axleIndex == 2) {
                        position.set(Position.KEY_AXLE_WEIGHT, buf.readUnsignedIntLE());
                    } else {
                        position.set("axleLoad" + axleIndex, buf.readUnsignedIntLE());
                    }
                    break;

                }

                case 69: {
                    position.set(Position.KEY_ENGINE_LOAD, buf.readUnsignedByte());
                    position.set(Position.KEY_THROTTLE, buf.readUnsignedByte());
                    int motionBits = buf.readUnsignedShortLE();
                    if (motionBits != 0) {
                        position.set("motionState", motionBits);
                    }
                    break;
                }

                case 71:
                case 72:
                case 73:
                case 74:
                case 75:
                case 76:
                case 77:
                case 78:
                case 79: {
                    int idx = tag - 70;
                    int level = buf.readUnsignedShortLE();
                    int tempRaw = buf.readUnsignedShortLE();
                    if (level != 0x0000 && level != 0xFFFF) {
                        position.set("rawFuelLevel" + idx, level);
                        position.set("llsTemperture" + idx, (tempRaw - 100.0) / 10.0);
                    }
                    break;
                }

                case 151: {
                    position.set(Position.KEY_HDOP, buf.readUnsignedShortLE() / 100.0);
                    buf.skipBytes(2);
                    break;
                }

                default:
                    buf.skipBytes(4);
                    break;
            }

            readBytes += 5;
        }

        position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid, rssi)));

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        if (!buf.isReadable()) {
            return null;
        }

        int startSign = buf.readUnsignedByte();

        if (startSign == HEADER_START_SIGN) {

            int version = buf.readUnsignedByte();
            long imeiLong = buf.readLongLE();

            if (version == HEADER_VERSION_3 && buf.readableBytes() >= 8) {
                buf.skipBytes(8);
            }

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(imeiLong));
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
        int packageNumber = buf.readUnsignedByte();

        while (buf.readableBytes() > 1) {

            int recordType = buf.readUnsignedByte();

            switch (recordType) {
                case RECORD_PING, RECORD_DATA, RECORD_TEXT, RECORD_FILE, RECORD_BINARY: {
                    int length = buf.readUnsignedShortLE();
                    Date time = new Date(buf.readUnsignedIntLE() * 1000);
                    if (recordType == RECORD_DATA) {
                        positions.add(decodePosition(deviceSession, buf, length, time));
                    } else {
                        buf.skipBytes(length);
                    }
                    buf.readUnsignedByte();
                    break;
                }
                default:
                    return positions.isEmpty() ? null : positions;
            }
        }

        sendResponse(channel, HEADER_VERSION_2, packageNumber);

        return positions.isEmpty() ? null : positions;
    }

}
