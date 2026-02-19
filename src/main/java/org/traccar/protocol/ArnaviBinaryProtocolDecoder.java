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

    // Frame markers
    private static final int HEADER_START_SIGN  = 0xFF;
    private static final int PACKAGE_START_SIGN = 0x5B;
    private static final int PACKAGE_END_SIGN   = 0x5D;

    // HEADER protocol versions
    private static final int HEADER_VERSION_1 = 0x22; // legacy
    private static final int HEADER_VERSION_2 = 0x23; // GSM/GPRS
    private static final int HEADER_VERSION_3 = 0x24; // EXT ID (has extra 8-byte WiFi ID)
    private static final int HEADER_VERSION_4 = 0x25; // WiFi

    // PACKET content types
    private static final int RECORD_PING   = 0x00;
    private static final int RECORD_DATA   = 0x01;
    private static final int RECORD_TEXT   = 0x03;
    private static final int RECORD_FILE   = 0x04;
    private static final int RECORD_BINARY = 0x06;

    public ArnaviBinaryProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    /**
     * Send SERVER_COM acknowledgement to the device.
     *
     * HEADER response:
     *   v1:       7B 00 00 7D
     *   v2/v3/v4: 7B 04 00 <CS> <unix_LE4> 7D
     *
     * PACKAGE response:
     *   7B 00 <pkg_num> 7D
     */
    private void sendResponse(Channel channel, int version, int packageNumber) {
        if (channel == null) {
            return;
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x7B); // SERVER_COM start

        if (packageNumber == 0) {
            // Acknowledging HEADER
            if (version == HEADER_VERSION_1) {
                response.writeByte(0x00);
                response.writeByte(0x00);
            } else {
                // N=4 (4 bytes of unixtime follow after CS)
                response.writeByte(0x04);
                response.writeByte(0x00); // parcel number 0x00 = HEADER

                long unixTime = System.currentTimeMillis() / 1000;
                ByteBuf timeBytes = Unpooled.buffer(4);
                timeBytes.writeIntLE((int) unixTime);

                response.writeByte(Checksum.modulo256(timeBytes.nioBuffer()));
                response.writeBytes(timeBytes);
                timeBytes.release();
            }
        } else {
            // Acknowledging PACKAGE
            response.writeByte(0x00);
            response.writeByte((byte) packageNumber);
        }

        response.writeByte(0x7D); // SERVER_COM end
        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
    }

    private Position decodePosition(DeviceSession deviceSession, ByteBuf buf, int length, Date time) {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(time);

        int readBytes = 0;
        while (readBytes < length) {

            if (buf.readableBytes() < 5) {
                break; // incomplete tag — stop safely
            }

            int tag = buf.readUnsignedByte();

            switch (tag) {

                // ── TAG 0x01 – External voltage (mV) + internal battery (mV) ────
                // Bytes 0-1: battery voltage LE uint16; 0x0000 = not connected
                // Bytes 2-3: external voltage LE uint16; 0x0000 = not connected, 0xFFFF = error
                case 0x01: {
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

                // ── TAG 0x03 – Latitude, IEEE 754 float LE ──────────────────────
                // Positive = North, negative = South
                case 0x03: {
                    float latitude = buf.readFloatLE();
                    if (Float.compare(latitude, 0.0f) != 0
                            && latitude >= -90.0f && latitude <= 90.0f) {
                        position.setLatitude(latitude);
                    }
                    break;
                }

                // ── TAG 0x04 – Longitude, IEEE 754 float LE ─────────────────────
                // Positive = East, negative = West
                case 0x04: {
                    float longitude = buf.readFloatLE();
                    if (Float.compare(longitude, 0.0f) != 0
                            && longitude >= -180.0f && longitude <= 180.0f) {
                        position.setLongitude(longitude);
                    }
                    break;
                }

                // ── TAG 0x05 – Navigation parameters ────────────────────────────
                // Byte 0: course = value * 2 (degrees)
                // Byte 1: altitude = value * 10 (metres)
                // Byte 2: satellites — bits 0-3 GPS count, bits 4-7 GLONASS count
                //         0xFE = LBS position, 0xFF = WiFi position
                // Byte 3: speed in knots
                case 0x05: {
                    int course     = buf.readUnsignedByte() * 2;
                    int altitude   = buf.readUnsignedByte() * 10;
                    int satByte    = buf.readUnsignedByte();
                    int speedKnots = buf.readUnsignedByte();

                    if (satByte == 0xFE || satByte == 0xFF) {
                        // Coordinates come from LBS or WiFi — mark as invalid GPS
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

                // ── TAG 0x06 – Digital I/O: PIN / IMPS / FREQ / VOLT ────────────
                // Byte 0: mode  0x01=discrete  0x06=pulses  0x07=freq  0x08=analog mV
                // Byte 1: input number (irrelevant for mode 0x01)
                // Bytes 2-3: value LE uint16
                case 0x06: {
                    int mode = buf.readUnsignedByte();

                    switch (mode) {
                        case 0x01: {
                            int virtualSensors = buf.readUnsignedByte();  // byte[1]
                            int physicalInputs = buf.readUnsignedShortLE(); // bytes[2-3]
                            position.set("virtualignition", BitUtil.check(virtualSensors, 0));
                            //position.set(Position.KEY_IGNITION, virtualIgnition); //optional | опционально, лучше использовать вычисляемый атрибут
                            //Недокументированные особенности работы virtual ignition:
                            //1 - Зажигание по напряжению, если нет, то:
                            //2 - зажигание по CAN, если нет, то:
                            //3 - зажигание по состоянию входа с типом "Зажигание"
                            position.set("callButton", BitUtil.check(virtualSensors, 1));
                            position.set(Position.PREFIX_IN, physicalInputs);
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
                            buf.skipBytes(3); // byte[1] + bytes[2-3]
                            break;
                    }
                    break;
                }

                // ── TAG 0x07 – SIM1 LAC + CID ───────────────────────────────────
                // Bytes 0-1: CID LE uint16
                // Bytes 2-3: LAC LE uint16
                case 0x07: {
                    int cid = buf.readUnsignedShortLE();
                    int lac = buf.readUnsignedShortLE();
                    position.set("lac", lac);
                    position.set("cid", cid);
                    break;
                }

                // ── TAG 0x08 – SIM1 GSM signal + MCC + MNC ──────────────────────
                // Byte 0:    MNC
                // Bytes 1-2: MCC LE uint16
                // Byte 3:    signal level 0-31
                case 0x08: {
                    int mnc    = buf.readUnsignedByte();
                    int mcc    = buf.readUnsignedShortLE();
                    int signal = buf.readUnsignedByte();
                    position.set(Position.KEY_RSSI, signal);
                    position.set("mcc", mcc);
                    position.set("mnc", mnc);
                    break;
                }

                // ── TAG 0x09 – Device Status (DS) bitmask ───────────────────────
                case 0x09: {
                    long status = buf.readUnsignedIntLE();
                    position.set(Position.KEY_STATUS, status);
                    
                    //Digital IN IN0–IN7 (bits 0–7)
                    int inputs = (int) (status & 0xFF);
                    position.set(Position.KEY_INPUT, inputs);

                    // Digital OUT OUT0–OUT3 (bits 8–11)
                    int outputs = (int) ((status >> 8) & 0x0F);
                    position.set(Position.KEY_OUTPUT, outputs);

                    // Состояние GSM модема (bits 12–13)
                    int gsmState = (int) ((status >> 12) & 0x03);
                    position.set("gsmState", gsmState);

                    // Состояние GPS/ГЛОНАСС (bits 14–15)
                    int gpsState = (int) ((status >> 14) & 0x03);
                    position.set("gpsState", gpsState);

                    // Датчик движения (bit 16)
                    position.set(Position.KEY_MOTION, BitUtil.check(status, 16));

                    // Наличие SIM (bit 18)
                    position.set("simPresent", BitUtil.check(status, 18));

                    // st0 режим охраны (bit 19)
                    position.set("guardMode", BitUtil.check(status, 19));

                    // st1 SOS/тревога (bit 20)
                    position.set("alarm", BitUtil.check(status, 20)
                                ? Position.ALARM_SOS : null);
                    break;
                }

                // ── TAG 0x33 (51) – CAN security / status flags ─────────────────
                // Bitmask; store raw for downstream processing
                case 0x33: {
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

                // ── TAG 0x34 (52) – CAN total engine operating time ──────────────
                // Value = hours * 100
                case 0x34: {
                    long engineHours100 = buf.readUnsignedIntLE();
                    position.set(Position.KEY_HOURS, (long) (engineHours100 / 100.0 * 3600 * 1000));// Traccar KEY_HOURS expects milliseconds
                    break;
                }

                // ── TAG 0x35 (53) – CAN total odometer ──────────────────────────
                // Value = km * 100
                case 0x35: {
                    long odo100 = buf.readUnsignedIntLE();
                    position.set(Position.KEY_ODOMETER, (long) (odo100 / 100.0 * 1000));// Traccar KEY_ODOMETER expects metres
                    break;
                }

                // ── TAG 0x36 (54) – CAN total fuel consumed ─────────────────────
                // Value = litres * 10
                case 0x36: {
                    long fuelused = buf.readUnsignedIntLE();
                    position.set(Position.KEY_FUEL_USED, fuelused / 10.0);
                    break;
                }

                // ── TAG 0x37 (55) – CAN fuel level in tank (%) ──────────────────
                // Value = percent * 10  (e.g. 594 → 59.4 %)
                case 0x37: {
                    int fuel = buf.readUnsignedShortLE();
                    buf.skipBytes(2);
                    position.set(Position.KEY_FUEL_LEVEL, fuel / 10.0);
                    break;
                }

                // ── TAG 0x38 (56) – CAN fuel level in tank (litres) ─────────────
                case 0x38: {
                    long fuelL = buf.readUnsignedIntLE();
                    position.set("fuelLitres", fuelL);
                    break;
                }

                // ── TAG 0x39 (57) – CAN engine speed (RPM) ──────────────────────
                case 0x39: {
                    int rpm = buf.readUnsignedShortLE();
                    buf.skipBytes(2);
                    position.set(Position.KEY_RPM, rpm);
                    break;
                }

                // ── TAG 0x3A (58) – CAN engine temperature (°C) ─────────
                case 0x3A: {
                    int enginetemp = (int) buf.readUnsignedIntLE();
                    position.set(Position.KEY_ENGINE_TEMP, enginetemp);
                    break;
                }

                // ── TAG 0x3B (59) – CAN vehicle speed (km/h) ────────────────────
                case 0x3B: {
                    int canSpeed = (int) buf.readUnsignedIntLE();
                    position.set(Position.KEY_OBD_SPEED, canSpeed);
                    break;
                }

                // ── TAGs 0x3C–0x40 (60–64) – CAN axle load 1–5 (kg) ────────────
                case 0x3C:
                case 0x3D:
                case 0x3E:
                case 0x3F:
                case 0x40: {
                    int axleIndex = tag - 0x3C + 1; // 1..5
                    long axleLoad = buf.readUnsignedIntLE();
                    position.set("axleLoad" + axleIndex, axleLoad);
                    break;
                }

                // ── TAG 0x45 (69) – CAN driving state / engine load ─────────────
                // Byte 0: gas pedal position (0–100 %)
                // Byte 1: engine load (0–100 %)
                // Bytes 2-3: motion characteristic bitmask (cfm16..cfm31)
                case 0x45: {
                    int throttle      = buf.readUnsignedByte();
                    int engineLoadPct = buf.readUnsignedByte();
                    int motionBits    = buf.readUnsignedShortLE(); //по документации используется только в сельхоз технике, для обычного транспорта всегда 0
                    position.set(Position.KEY_ENGINE_LOAD, engineLoadPct);
                    position.set(Position.KEY_THROTTLE, throttle);
                    
                    if (motionBits !=0) {
                        position.set("motionState", motionBits);   
                    }
                    break;
                }

                // ── TAGs 0x5C–0x60 (91–96) – LLS fuel-level sensors 1–5 ─────────
                // Bytes 0-1: level raw value (LE uint16)
                // 0xFFFF = sensor disconnected or error — skip entirely
                // Bytes 2-3: temperature raw; decoded as (raw - 100) / 10 °C
                // 0xFFFF = temperature not available
                case 0x5C:
                case 0x5D:
                case 0x5E:
                case 0x5F:
                case 0x60: {
                    int idx     = tag - 0x5B;
                    int level   = buf.readUnsignedShortLE() & 0xFFFF;
                    int tempRaw = buf.readUnsignedShortLE() & 0xFFFF;
                    if (level != 0x0000 && level != 0xFFFF) {
                    position.set(Position.KEY_FUEL + idx, level);
                    position.set(Position.PREFIX_TEMP + idx, (tempRaw - 100.0) / 10.0);
                    }
                    break;
                }

                // ── TAG 0x97 (151) – Dilution of Precision (HDOP) ───────────────
                // Bits  0-15: HDOP * 100 (LE uint16)
                // Bits 16-31: reserved
                case 0x97: {
                    int hdopRaw = buf.readUnsignedShortLE();
                    buf.skipBytes(2); // reserved
                    position.set(Position.KEY_HDOP, hdopRaw / 100.0);
                    break;
                }

                // ── All unknown tags: skip 4 value bytes ─────────────────────────
                default:
                    buf.skipBytes(4);
                    break;
            }

            readBytes += 5; // 1 tag byte + 4 value bytes always
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

        // ── HEADER (device identification at session start) ─────────────────────
        if (startSign == HEADER_START_SIGN) {

            int version = buf.readUnsignedByte();

            // IMEI: 8 bytes little-endian (15-digit IMEI fits in 7 bytes; 8th byte is 0x00)
            long imeiLong = buf.readLongLE();

            // HEADER3 carries an additional 8-byte extended WiFi identifier after IMEI
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

        // ── PACKAGE frame (0x5B … 0x5D) ────────────────────────────────────────
        if (startSign != PACKAGE_START_SIGN) {
            return null; // unknown frame start — discard
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        List<Position> positions = new LinkedList<>();
        int packageNumber = buf.readUnsignedByte(); // 0x01–0xFB

        // Iterate over PACKETs until the 0x5D end marker or end of buffer
        while (buf.isReadable()) {

            int recordType = buf.readUnsignedByte();

            if (recordType == PACKAGE_END_SIGN) {
                break; // clean end of PACKAGE
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
                    buf.readUnsignedByte(); // checksum (verified by framing layer)
                    break;
                }
                default:
                    // Unknown record type — cannot recover, return what we have
                    return positions.isEmpty() ? null : positions;
            }
        }

        sendResponse(channel, HEADER_VERSION_2, packageNumber);

        return positions.isEmpty() ? null : positions;
    }

}
