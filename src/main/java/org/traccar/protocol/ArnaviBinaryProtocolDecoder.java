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
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ArnaviBinaryProtocolDecoder extends BaseProtocolDecoder {

    private static final int HEADER_START_SIGN  = 0xFF;
    private static final int PACKAGE_START_SIGN = 0x5B;
    private static final int PACKAGE_END_SIGN   = 0x5D;

    private static final int HEADER_VERSION_1 = 0x22;
    private static final int HEADER_VERSION_2 = 0x23;
    private static final int HEADER_VERSION_3 = 0x24;
    private static final int HEADER_VERSION_4 = 0x25;
    
    private static final int RECORD_PING   = 0x00;
    private static final int RECORD_DATA   = 0x01;
    private static final int RECORD_TEXT   = 0x03;
    private static final int RECORD_FILE   = 0x04;
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
                    float latitude = buf.readFloatLE();
                    if (latitude > -90.0f && latitude < 90.0f) {
                        position.setLatitude(latitude);
                    }
                    break;
                }

                case 4: {
                    float longitude = buf.readFloatLE();
                    if (longitude > -180.0f && longitude < 180.0f) {
                        position.setLongitude(longitude);
                    }
                    break;
                }

                case 5: {
                    int course     = buf.readUnsignedByte() * 2;
                    int altitude   = buf.readUnsignedByte() * 10;
                    int satByte    = buf.readUnsignedByte();
                    int speedKnots = buf.readUnsignedByte();

                    if (satByte == 0xFE || satByte == 0xFF) {
                        position.setValid(false);
                    } else {
                        int gpsSat = satByte & 0x0F;
                        int gloSat = (satByte >> 4) & 0x0F;
                        position.set(Position.KEY_SATELLITES, gpsSat + gloSat);
                        position.setCourse(course);
                        position.setAltitude(altitude);
                        position.setSpeed(speedKnots);
                        position.setValid(true);
                    }

                    break;
                }

                case 6: {
                    int mode = buf.readUnsignedByte();

                    switch (mode) {
                        case 0x01: {
                            int virtualSensors = buf.readUnsignedByte();
                            int physicalInputs = buf.readUnsignedShortLE();
                            position.set("virtualignition", BitUtil.check(virtualSensors, 0));
                            position.set("callButton", BitUtil.check(virtualSensors, 1));
                            for (int i = 0; i < 8; i++) {
                            position.set(Position.PREFIX_IN + (i + 1), (physicalInputs & (1 << i)) != 0);
                            }
                            break;
                        }
                        case 0x06: {
                            int inputNum = buf.readUnsignedByte();
                            int value    = buf.readUnsignedShortLE();
                            position.set("pulses" + inputNum, value);
                            break;
                        }
                        case 0x07: {
                            int inputNum = buf.readUnsignedByte();
                            int value    = buf.readUnsignedShortLE();
                            position.set("freq" + inputNum, value);
                            break;
                        }
                        case 0x08: {
                            int inputNum = buf.readUnsignedByte();
                            int value    = buf.readUnsignedShortLE();
                            position.set(Position.PREFIX_ADC + inputNum, value / 1000.0);
                            break;
                        }
                        default:
                            buf.skipBytes(3);
                            break;
                    }
                    break;
                }

                case 7: {
                    int cid = buf.readUnsignedShortLE();
                    int lac = buf.readUnsignedShortLE();
                    position.set("lac", lac);
                    position.set("cid", cid);
                    break;
                }

                case 8: {
                    int mnc    = buf.readUnsignedByte();
                    int mcc    = buf.readUnsignedShortLE();
                    int signal = buf.readUnsignedByte();
                    position.set(Position.KEY_RSSI, signal);
                    position.set("mcc", mcc);
                    position.set("mnc", mnc);
                    break;
                }

                case 9: {
                    long status = buf.readUnsignedIntLE();
                    position.set(Position.KEY_STATUS, status);

                    int inputs = (int) (status & 0xFF);
                    position.set(Position.KEY_INPUT, inputs);

                    int outputs = (int) ((status >> 8) & 0x0F);
                    position.set(Position.KEY_OUTPUT, outputs);

                    int gsmState = (int) ((status >> 12) & 0x03);
                    position.set("gsmState", gsmState);

                    int gpsState = (int) ((status >> 14) & 0x03);
                    position.set("gpsState", gpsState);
                    position.set(Position.KEY_MOTION, BitUtil.check(status, 16));
                    position.set("simPresent", BitUtil.check(status, 18));
                    position.set("guardMode", BitUtil.check(status, 19));
                    position.set("alarm", BitUtil.check(status, 20)
                                ? Position.ALARM_SOS : null);
                    break;
                }

                case 51: {
                    long flags = buf.readUnsignedIntLE();
                    position.set("doorDriver",      BitUtil.check(flags, 8));
                    position.set("doorPassenger",   BitUtil.check(flags, 9));
                    position.set("trunkOpen",       BitUtil.check(flags, 10));
                    position.set("hoodOpen",        BitUtil.check(flags, 11));
                    position.set("handbrake",       BitUtil.check(flags, 12));
                    position.set("brakePedal",      BitUtil.check(flags, 13));
                    position.set("engineRunning",   BitUtil.check(flags, 14));
                    position.set("canIgnition",     BitUtil.check(flags, 16));
                    position.set("keyInIgnition",   BitUtil.check(flags, 19));
                    break;
                }

                case 52: {
                    long engineHours100 = buf.readUnsignedIntLE();
                    position.set(Position.KEY_HOURS, (long) (engineHours100 / 100.0 * 3600 * 1000));
                    break;
                }

                case 53: {
                    long odo100 = buf.readUnsignedIntLE();
                    position.set(Position.KEY_ODOMETER, (long) (odo100 / 100.0 * 1000));
                    break;
                }

                case 54: {
                    long fuelused = buf.readUnsignedIntLE();
                    position.set(Position.KEY_FUEL_USED, fuelused / 10.0);
                    break;
                }

                case 55: {
                    int fuel = buf.readUnsignedShortLE();
                    buf.skipBytes(2);
                    position.set(Position.KEY_FUEL_LEVEL, fuel / 10.0);
                    break;
                }

                case 56: {
                    long fuelL = buf.readUnsignedIntLE();
                    position.set("fuelLitres", fuelL);
                    break;
                }

                case 57: {
                    int rpm = buf.readUnsignedShortLE();
                    buf.skipBytes(2);
                    position.set(Position.KEY_RPM, rpm);
                    break;
                }

                case 58: {
                    int enginetemp = (int) buf.readUnsignedIntLE();
                    position.set(Position.KEY_ENGINE_TEMP, enginetemp);
                    break;
                }

                case 59: {
                    int canSpeed = (int) buf.readUnsignedIntLE();
                    position.set(Position.KEY_OBD_SPEED, canSpeed);
                    break;
                }

                case 60:
                case 61:
                case 62:
                case 63:
                case 64: {
                    int axleIndex = tag - 60 + 1;
                    long axleLoad = buf.readUnsignedIntLE();
                    if (axleIndex == 2) {
                        position.set(Position.KEY_AXLE_WEIGHT, axleLoad);
                    } else {
                    position.set("axleLoad" + axleIndex, axleLoad);
                    }
                    break;
                    
                }

                case 69: {
                    int throttle      = buf.readUnsignedByte();
                    int engineLoadPct = buf.readUnsignedByte();
                    int motionBits    = buf.readUnsignedShortLE();
                    position.set(Position.KEY_ENGINE_LOAD, engineLoadPct);
                    position.set(Position.KEY_THROTTLE, throttle);
                    
                    if (motionBits !=0) {
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
                case 79:{
                    int idx     = tag - 70;
                    int level   = buf.readUnsignedShortLE();
                    int tempRaw = buf.readUnsignedShortLE();
                    if (level != 0x0000 && level != 0xFFFF) {
                    position.set(Position.KEY_FUEL + idx, level);
                    position.set(Position.PREFIX_TEMP + idx, (tempRaw - 100.0) / 10.0);
                    }
                    break;
                }

                case 151: {
                    int hdopRaw = buf.readUnsignedShortLE();
                    buf.skipBytes(2); // reserved
                    position.set(Position.KEY_HDOP, hdopRaw / 100.0);
                    break;
                }

                default:
                    buf.skipBytes(4);
                    break;
            }

            readBytes += 5;
        }

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

            String imei = String.valueOf(imeiLong);
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                sendResponse(channel, version, 0);
            }
            return null;
        }

        if (startSign != PACKAGE_START_SIGN) {
            return null;
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        int packageNumber = buf.readUnsignedByte();

        while (buf.isReadable()) {

            int recordType = buf.readUnsignedByte();

            if (recordType == PACKAGE_END_SIGN) {
                break;
            }

            switch (recordType) {
                case RECORD_PING:
                case RECORD_DATA:
                case RECORD_TEXT:
                case RECORD_FILE:
                case RECORD_BINARY: {
                    int length = buf.readUnsignedShortLE();
                    Date time  = new Date(buf.readUnsignedIntLE() * 1000L);

                    if (recordType == RECORD_DATA && length > 0) {
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
