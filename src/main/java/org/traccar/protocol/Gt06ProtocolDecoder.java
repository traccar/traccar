/*
 * Copyright 2012 - 2024 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.BufferUtil;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Gt06ProtocolDecoder extends BaseProtocolDecoder {

    private final Map<Integer, ByteBuf> photos = new HashMap<>();

    public Gt06ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x10;
    public static final int MSG_GPS_LBS_6 = 0x11;
    public static final int MSG_GPS_LBS_1 = 0x12;
    public static final int MSG_GPS_LBS_2 = 0x22;
    public static final int MSG_GPS_LBS_3 = 0x37;
    public static final int MSG_GPS_LBS_4 = 0x2D;
    public static final int MSG_STATUS = 0x13;
    public static final int MSG_SATELLITE = 0x14;
    public static final int MSG_STRING = 0x15;
    public static final int MSG_GPS_LBS_STATUS_1 = 0x16;
    public static final int MSG_WIFI = 0x17;
    public static final int MSG_GPS_LBS_RFID = 0x17;
    public static final int MSG_GPS_LBS_STATUS_2 = 0x26;
    public static final int MSG_GPS_LBS_STATUS_3 = 0x27;
    public static final int MSG_LBS_MULTIPLE_1 = 0x28;
    public static final int MSG_LBS_MULTIPLE_2 = 0x2E;
    public static final int MSG_LBS_MULTIPLE_3 = 0x24;
    public static final int MSG_LBS_WIFI = 0x2C;
    public static final int MSG_LBS_EXTEND = 0x18;
    public static final int MSG_LBS_STATUS = 0x19;
    public static final int MSG_GPS_PHONE = 0x1A;
    public static final int MSG_GPS_LBS_EXTEND = 0x1E;     // JI09
    public static final int MSG_HEARTBEAT = 0x23;          // GK310
    public static final int MSG_ADDRESS_REQUEST = 0x2A;    // GK310
    public static final int MSG_ADDRESS_RESPONSE = 0x97;   // GK310
    public static final int MSG_GPS_LBS_5 = 0x31;          // AZ735 & SL4X
    public static final int MSG_GPS_LBS_STATUS_4 = 0x32;   // AZ735 & SL4X
    public static final int MSG_WIFI_5 = 0x33;             // AZ735 & SL4X
    public static final int MSG_LBS_3 = 0x34;              // SL4X
    public static final int MSG_AZ735_GPS = 0x32;          // AZ735 (extended)
    public static final int MSG_AZ735_ALARM = 0x33;        // AZ735 (only extended)
    public static final int MSG_X1_GPS = 0x34;
    public static final int MSG_X1_PHOTO_INFO = 0x35;
    public static final int MSG_X1_PHOTO_DATA = 0x36;
    public static final int MSG_STATUS_2 = 0x36;           // Jimi IoT 4G
    public static final int MSG_WIFI_2 = 0x69;
    public static final int MSG_GPS_MODULAR = 0x70;
    public static final int MSG_WIFI_4 = 0xF3;
    public static final int MSG_COMMAND_0 = 0x80;
    public static final int MSG_COMMAND_1 = 0x81;
    public static final int MSG_COMMAND_2 = 0x82;
    public static final int MSG_TIME_REQUEST = 0x8A;       // GK310
    public static final int MSG_INFO = 0x94;
    public static final int MSG_SERIAL = 0x9B;
    public static final int MSG_STRING_INFO = 0x21;
    public static final int MSG_GPS_LBS_7 = 0xA0;          // GK310 & JM-VL03
    public static final int MSG_LBS_2 = 0xA1;              // GK310
    public static final int MSG_WIFI_3 = 0xA2;             // GK310
    public static final int MSG_GPS_LBS_STATUS_5 = 0xA2;   // LWxG
    public static final int MSG_FENCE_SINGLE = 0xA3;       // GK310
    public static final int MSG_FENCE_MULTI = 0xA4;        // GK310 & JM-LL301
    public static final int MSG_LBS_ALARM = 0xA5;          // GK310 & JM-LL301
    public static final int MSG_LBS_ADDRESS = 0xA7;        // GK310
    public static final int MSG_OBD = 0x8C;                // FM08ABC
    public static final int MSG_DTC = 0x65;                // FM08ABC
    public static final int MSG_PID = 0x66;                // FM08ABC
    public static final int MSG_BMS = 0x40;                // WD-209
    public static final int MSG_MULTIMEDIA = 0x41;         // WD-209
    public static final int MSG_ALARM = 0x95;              // JC100

    private enum Variant {
        VXT01,
        WANWAY_S20,
        SR411_MINI,
        GT06E_CARD,
        BENWAY,
        S5,
        SPACE10X,
        STANDARD,
        OBD6,
        WETRUST,
        JC400,
        SL4X,
        SEEWORLD,
        RFID,
        LW4G,
    }

    private Variant variant;

    private static final Pattern PATTERN_FUEL = new PatternBuilder()
            .text("!AIOIL,")
            .number("d+,")                       // device address
            .number("d+.d+,")                    // output value
            .number("(d+.d+),")                  // temperature
            .expression("[^,]+,")                // version
            .number("dd")                        // back wave
            .number("d")                         // software status code
            .number("d,")                        // hardware status code
            .number("(d+.d+),")                  // measured value
            .expression("[01],")                 // movement status
            .number("d+,")                       // excited wave times
            .number("xx")                        // checksum
            .compile();

    private static final Pattern PATTERN_LOCATION = new PatternBuilder()
            .text("Current position!")
            .number("Lat:([NS])(d+.d+),")        // latitude
            .number("Lon:([EW])(d+.d+),")        // longitude
            .text("Course:").number("(d+.d+),")  // course
            .text("Speed:").number("(d+.d+),")   // speed
            .text("DateTime:")
            .number("(dddd)-(dd)-(dd) +")        // date
            .number("(dd):(dd):(dd)")            // time
            .compile();

    private static boolean isSupported(int type) {
        return hasGps(type) || hasLbs(type) || hasStatus(type);
    }

    private static boolean hasGps(int type) {
        switch (type) {
            case MSG_GPS:
            case MSG_GPS_LBS_1:
            case MSG_GPS_LBS_2:
            case MSG_GPS_LBS_3:
            case MSG_GPS_LBS_4:
            case MSG_GPS_LBS_5:
            case MSG_GPS_LBS_6:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_GPS_LBS_STATUS_4:
            case MSG_GPS_LBS_STATUS_5:
            case MSG_GPS_PHONE:
            case MSG_GPS_LBS_EXTEND:
            case MSG_GPS_LBS_7:
            case MSG_GPS_LBS_RFID:
            case MSG_FENCE_SINGLE:
            case MSG_FENCE_MULTI:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasLbs(int type) {
        switch (type) {
            case MSG_LBS_STATUS:
            case MSG_GPS_LBS_1:
            case MSG_GPS_LBS_2:
            case MSG_GPS_LBS_3:
            case MSG_GPS_LBS_4:
            case MSG_GPS_LBS_5:
            case MSG_GPS_LBS_6:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_GPS_LBS_STATUS_4:
            case MSG_GPS_LBS_STATUS_5:
            case MSG_GPS_LBS_7:
            case MSG_GPS_LBS_RFID:
            case MSG_FENCE_SINGLE:
            case MSG_FENCE_MULTI:
            case MSG_LBS_ALARM:
            case MSG_LBS_ADDRESS:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasStatus(int type) {
        switch (type) {
            case MSG_STATUS:
            case MSG_STATUS_2:
            case MSG_LBS_STATUS:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_GPS_LBS_STATUS_4:
            case MSG_GPS_LBS_STATUS_5:
            case MSG_FENCE_MULTI:
            case MSG_LBS_ALARM:
                return true;
            default:
                return false;
        }
    }

    private void sendResponse(Channel channel, boolean extended, int type, int index, ByteBuf content) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            int length = 5 + (content != null ? content.readableBytes() : 0);
            if (extended) {
                response.writeShort(0x7979);
                response.writeShort(length);
            } else {
                response.writeShort(0x7878);
                response.writeByte(length);
            }
            response.writeByte(type);
            if (content != null) {
                response.writeBytes(content);
                content.release();
            }
            response.writeShort(index);
            response.writeShort(Checksum.crc16(Checksum.CRC16_X25,
                    response.nioBuffer(2, response.writerIndex() - 2)));
            response.writeByte('\r');
            response.writeByte('\n');
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private void sendPhotoRequest(Channel channel, int pictureId) {
        ByteBuf photo = photos.get(pictureId);
        ByteBuf content = Unpooled.buffer();
        content.writeInt(pictureId);
        content.writeInt(photo.writerIndex());
        content.writeShort(Math.min(photo.writableBytes(), 1024));
        sendResponse(channel, false, MSG_X1_PHOTO_DATA, 0, content);
    }

    public static boolean decodeGps(Position position, ByteBuf buf, boolean hasLength, TimeZone timezone) {
        return decodeGps(position, buf, hasLength, true, true, false, timezone);
    }

    public static boolean decodeGps(
            Position position, ByteBuf buf, boolean hasLength, boolean hasSatellites,
            boolean hasSpeed, boolean longSpeed, TimeZone timezone) {

        DateBuilder dateBuilder = new DateBuilder(timezone)
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        if (hasLength && buf.readUnsignedByte() == 0) {
            return false;
        }

        if (hasSatellites) {
            position.set(Position.KEY_SATELLITES, BitUtil.to(buf.readUnsignedByte(), 4));
        }

        double latitude = buf.readUnsignedInt() / 60.0 / 30000.0;
        double longitude = buf.readUnsignedInt() / 60.0 / 30000.0;

        if (hasSpeed) {
            position.setSpeed(UnitsConverter.knotsFromKph(
                    longSpeed ? buf.readUnsignedShort() : buf.readUnsignedByte()));
        }

        int flags = buf.readUnsignedShort();
        position.setCourse(BitUtil.to(flags, 10));
        position.setValid(BitUtil.check(flags, 12));

        if (!BitUtil.check(flags, 10)) {
            latitude = -latitude;
        }
        if (BitUtil.check(flags, 11)) {
            longitude = -longitude;
        }

        position.setLatitude(latitude);
        position.setLongitude(longitude);

        if (BitUtil.check(flags, 14)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(flags, 15));
        }

        return true;
    }

    private boolean decodeLbs(Position position, ByteBuf buf, int type, boolean hasLength) {

        int length = 0;
        if (hasLength) {
            length = buf.readUnsignedByte();
            if (length == 0) {
                boolean zeroedData = true;
                for (int i = buf.readerIndex() + 9; i < buf.readerIndex() + 45 && i < buf.writerIndex(); i++) {
                    if (buf.getByte(i) != 0) {
                        zeroedData = false;
                        break;
                    }
                }
                if (zeroedData) {
                    buf.skipBytes(Math.min(buf.readableBytes(), 45));
                }
                return false;
            }
        }

        int mcc = buf.readUnsignedShort();
        int mnc;
        if (BitUtil.check(mcc, 15) || type == MSG_GPS_LBS_6 || variant == Variant.SL4X) {
            mnc = buf.readUnsignedShort();
        } else {
            mnc = buf.readUnsignedByte();
        }
        int lac;
        if (type == MSG_LBS_ALARM || type == MSG_GPS_LBS_7 || type == MSG_GPS_LBS_STATUS_5) {
            lac = buf.readInt();
        } else {
            lac = buf.readUnsignedShort();
        }
        long cid;
        if (type == MSG_LBS_ALARM || type == MSG_GPS_LBS_7 || variant == Variant.SL4X || type == MSG_GPS_LBS_STATUS_5) {
            cid = buf.readLong();
        } else if (type == MSG_GPS_LBS_6 || variant == Variant.SEEWORLD) {
            cid = buf.readUnsignedInt();
        } else {
            cid = buf.readUnsignedMedium();
        }

        position.setNetwork(new Network(CellTower.from(BitUtil.to(mcc, 15), mnc, lac, cid)));

        if (length > 9) {
            buf.skipBytes(length - 9);
        }

        return true;
    }

    private void decodeStatus(Position position, ByteBuf buf) {

        int status = buf.readUnsignedByte();

        position.set(Position.KEY_STATUS, status);
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 1));
        position.set(Position.KEY_CHARGE, BitUtil.check(status, 2));
        position.set(Position.KEY_BLOCKED, BitUtil.check(status, 7));

        switch (BitUtil.between(status, 3, 6)) {
            case 1:
                position.addAlarm(Position.ALARM_VIBRATION);
                break;
            case 2:
                position.addAlarm(Position.ALARM_POWER_CUT);
                break;
            case 3:
                position.addAlarm(Position.ALARM_LOW_BATTERY);
                break;
            case 4:
                position.addAlarm(Position.ALARM_SOS);
                break;
            case 6:
                position.addAlarm(Position.ALARM_GEOFENCE);
                break;
            case 7:
                if (variant == Variant.VXT01) {
                    position.addAlarm(Position.ALARM_OVERSPEED);
                } else {
                    position.addAlarm(Position.ALARM_REMOVING);
                }
                break;
            default:
                break;
        }
    }

    private String decodeAlarm(short value, boolean modelLW) {
        return switch (value) {
            case 0x01 -> Position.ALARM_SOS;
            case 0x02 -> Position.ALARM_POWER_CUT;
            case 0x03, 0x09 -> Position.ALARM_VIBRATION;
            case 0x04 -> Position.ALARM_GEOFENCE_ENTER;
            case 0x05 -> Position.ALARM_GEOFENCE_EXIT;
            case 0x06 -> Position.ALARM_OVERSPEED;
            case 0x0E, 0x0F -> Position.ALARM_LOW_BATTERY;
            case 0x11 -> Position.ALARM_POWER_OFF;
            case 0x0C, 0x13, 0x25 -> Position.ALARM_TAMPERING;
            case 0x14 -> Position.ALARM_DOOR;
            case 0x18 -> modelLW ? Position.ALARM_ACCIDENT : Position.ALARM_REMOVING;
            case 0x19 -> modelLW ? Position.ALARM_ACCELERATION : Position.ALARM_LOW_BATTERY;
            case 0x1A, 0x28 -> Position.ALARM_BRAKING;
            case 0x1B, 0x2A, 0x2B, 0x2E -> Position.ALARM_CORNERING;
            case 0x23 -> Position.ALARM_FALL_DOWN;
            case 0x29 -> Position.ALARM_ACCELERATION;
            case 0x2C -> Position.ALARM_ACCIDENT;
            case 0x30 -> Position.ALARM_JAMMING;
            default -> null;
        };
    }

    private Object decodeBasic(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        int length = buf.readUnsignedByte();
        int dataLength = length - 5;
        int type = buf.readUnsignedByte();

        Position position = new Position(getProtocolName());
        DeviceSession deviceSession = null;
        if (type != MSG_LOGIN) {
            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }
            position.setDeviceId(deviceSession.getDeviceId());
            if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
                deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId()));
            }
        }

        String model = deviceSession != null ? getDeviceModel(deviceSession) : null;
        boolean modelLW = model != null && model.toUpperCase().startsWith("LW");

        if (type == MSG_LOGIN) {

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            buf.readUnsignedShort(); // type

            deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null) {
                TimeZone timeZone = getTimeZone(deviceSession.getDeviceId(), null);
                if (timeZone == null && dataLength > 10) {
                    int extensionBits = buf.readUnsignedShort();
                    int hours = (extensionBits >> 4) / 100;
                    int minutes = (extensionBits >> 4) % 100;
                    int offset = (hours * 60 + minutes) * 60;
                    if ((extensionBits & 0x8) != 0) {
                        offset = -offset;
                    }
                    timeZone = TimeZone.getTimeZone("UTC");
                    timeZone.setRawOffset(offset * 1000);
                }
                deviceSession.set(DeviceSession.KEY_TIMEZONE, timeZone);

                sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);
            }

            return null;

        } else if (type == MSG_HEARTBEAT) {

            getLastLocation(position, null);

            int status = buf.readUnsignedByte();
            position.set(Position.KEY_ARMED, BitUtil.check(status, 0));
            position.set(Position.KEY_IGNITION, BitUtil.check(status, 1));
            position.set(Position.KEY_CHARGE, BitUtil.check(status, 2));

            if (buf.readableBytes() >= 2 + 6) {
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
            }
            if (buf.readableBytes() >= 1 + 6) {
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            }

            sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);

            return position;

        } else if (type == MSG_ADDRESS_REQUEST) {

            String response = "NA&&NA&&0##";
            ByteBuf content = Unpooled.buffer();
            content.writeByte(response.length());
            content.writeInt(0);
            content.writeBytes(response.getBytes(StandardCharsets.US_ASCII));
            sendResponse(channel, true, MSG_ADDRESS_RESPONSE, 0, content);

            return null;

        } else if (type == MSG_TIME_REQUEST) {

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            ByteBuf content = Unpooled.buffer();
            content.writeByte(calendar.get(Calendar.YEAR) - 2000);
            content.writeByte(calendar.get(Calendar.MONTH) + 1);
            content.writeByte(calendar.get(Calendar.DAY_OF_MONTH));
            content.writeByte(calendar.get(Calendar.HOUR_OF_DAY));
            content.writeByte(calendar.get(Calendar.MINUTE));
            content.writeByte(calendar.get(Calendar.SECOND));
            sendResponse(channel, false, MSG_TIME_REQUEST, 0, content);

            return null;

        } else if (type == MSG_X1_GPS && variant != Variant.SL4X) {

            buf.readUnsignedInt(); // data and alarm

            decodeGps(position, buf, false, deviceSession.get(DeviceSession.KEY_TIMEZONE));

            buf.readUnsignedShort(); // terminal info

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedByte(),
                    buf.readUnsignedShort(), buf.readUnsignedInt())));

            long driverId = buf.readUnsignedInt();
            if (driverId > 0) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, String.valueOf(driverId));
            }

            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);

            long portInfo = buf.readUnsignedInt();

            position.set(Position.KEY_INPUT, buf.readUnsignedByte());
            position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());

            for (int i = 1; i <= BitUtil.between(portInfo, 20, 24); i++) {
                position.set(Position.PREFIX_ADC + i, buf.readUnsignedShort() * 0.01);
            }

            return position;

        } else if (type == MSG_X1_PHOTO_INFO) {

            buf.skipBytes(6); // time
            buf.readUnsignedByte(); // fix status
            buf.readUnsignedInt(); // latitude
            buf.readUnsignedInt(); // longitude
            buf.readUnsignedByte(); // camera id
            buf.readUnsignedByte(); // photo source
            buf.readUnsignedByte(); // picture format

            ByteBuf photo = Unpooled.buffer(buf.readInt());
            int pictureId = buf.readInt();
            photos.put(pictureId, photo);
            sendPhotoRequest(channel, pictureId);

            return null;

        } else if ((type == MSG_WIFI && variant != Variant.RFID) || type == MSG_WIFI_2 || type == MSG_WIFI_4) {

            ByteBuf time = buf.readSlice(6);
            DateBuilder dateBuilder = new DateBuilder()
                    .setYear(BcdUtil.readInteger(time, 2))
                    .setMonth(BcdUtil.readInteger(time, 2))
                    .setDay(BcdUtil.readInteger(time, 2))
                    .setHour(BcdUtil.readInteger(time, 2))
                    .setMinute(BcdUtil.readInteger(time, 2))
                    .setSecond(BcdUtil.readInteger(time, 2));
            getLastLocation(position, dateBuilder.getDate());

            Network network = new Network();

            int wifiCount;
            if (type == MSG_WIFI_4) {
                wifiCount = buf.readUnsignedByte();
            } else {
                wifiCount = buf.getUnsignedByte(2);
            }

            for (int i = 0; i < wifiCount; i++) {
                if (type == MSG_WIFI_4) {
                    buf.skipBytes(2);
                }
                WifiAccessPoint wifiAccessPoint = new WifiAccessPoint();
                wifiAccessPoint.setMacAddress(String.format("%02x:%02x:%02x:%02x:%02x:%02x",
                        buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                        buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
                if (type != MSG_WIFI_4) {
                    wifiAccessPoint.setSignalStrength((int) buf.readUnsignedByte());
                }
                network.addWifiAccessPoint(wifiAccessPoint);
            }

            if (type != MSG_WIFI_4) {

                int cellCount = buf.readUnsignedByte();
                int mcc = buf.readUnsignedShort();
                int mnc = buf.readUnsignedByte();
                for (int i = 0; i < cellCount; i++) {
                    network.addCellTower(CellTower.from(
                            mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedByte()));
                }

                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeShort(0x7878);
                    response.writeByte(0);
                    response.writeByte(type);
                    response.writeBytes(time.resetReaderIndex());
                    response.writeByte('\r');
                    response.writeByte('\n');
                    channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
                }

            }

            position.setNetwork(network);

            return position;

        } else if (type == MSG_INFO) {

            getLastLocation(position, null);

            position.set(Position.KEY_POWER, buf.readShort() * 0.01);

            return position;

        } else if (type == MSG_LBS_MULTIPLE_3 && variant == Variant.SR411_MINI) {

            decodeGps(position, buf, false, deviceSession.get(DeviceSession.KEY_TIMEZONE));

            decodeLbs(position, buf, type, false);

            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
            position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);

            return position;

        } else if (type == MSG_LBS_MULTIPLE_1 || type == MSG_LBS_MULTIPLE_2 || type == MSG_LBS_MULTIPLE_3
                || type == MSG_LBS_EXTEND || type == MSG_LBS_WIFI || type == MSG_LBS_2 || type == MSG_LBS_3
                || (type == MSG_WIFI_3 && variant != Variant.LW4G) || type == MSG_WIFI_5) {

            DateBuilder dateBuilder = new DateBuilder((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE))
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

            getLastLocation(position, dateBuilder.getDate());

            if (variant == Variant.WANWAY_S20 || variant == Variant.SL4X) {
                buf.readUnsignedByte(); // ta
            }

            int mcc = buf.readUnsignedShort();
            int mnc = BitUtil.check(mcc, 15) || variant == Variant.SL4X
                    ? buf.readUnsignedShort() : buf.readUnsignedByte();
            Network network = new Network();

            int cellCount = variant == Variant.WANWAY_S20 ? buf.readUnsignedByte() : type == MSG_WIFI_5 ? 6 : 7;
            for (int i = 0; i < cellCount; i++) {
                int lac;
                int cid;
                if (type == MSG_LBS_2 || type == MSG_WIFI_3) {
                    lac = buf.readInt();
                    cid = (int) buf.readLong();
                } else if (type == MSG_WIFI_5 || type == MSG_LBS_3) {
                    lac = buf.readUnsignedShort();
                    cid = (int) buf.readUnsignedInt();
                } else {
                    lac = buf.readUnsignedShort();
                    cid = buf.readUnsignedMedium();
                }
                int rssi = -buf.readUnsignedByte();
                if (lac > 0) {
                    network.addCellTower(CellTower.from(BitUtil.to(mcc, 15), mnc, lac, cid, rssi));
                }
            }

            if (variant != Variant.WANWAY_S20 && variant != Variant.SL4X) {
                buf.readUnsignedByte(); // ta
            }

            if (type != MSG_LBS_MULTIPLE_1 && type != MSG_LBS_MULTIPLE_2 && type != MSG_LBS_MULTIPLE_3
                    && type != MSG_LBS_2 && type != MSG_LBS_3) {
                int wifiCount = buf.readUnsignedByte();
                for (int i = 0; i < wifiCount; i++) {
                    String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            mac.substring(0, mac.length() - 1), buf.readUnsignedByte()));
                }
            }

            position.setNetwork(network);

        } else if (type == MSG_STRING) {

            getLastLocation(position, null);

            int commandLength = buf.readUnsignedByte();

            if (commandLength > 0) {
                buf.readUnsignedInt(); // server flag (reserved)
                String data = buf.readSlice(commandLength - 4).toString(StandardCharsets.US_ASCII);
                if (data.startsWith("<ICCID:")) {
                    position.set(Position.KEY_ICCID, data.substring(7, 27));
                } else {
                    position.set(Position.KEY_RESULT, data);
                }
            }

        } else if (type == MSG_BMS) {

            buf.skipBytes(8); // serial number

            getLastLocation(position, new Date(buf.readUnsignedInt() * 1000));

            position.set("relativeCapacity", buf.readUnsignedByte());
            position.set("remainingCapacity", buf.readUnsignedShort());
            position.set("absoluteCapacity", buf.readUnsignedByte());
            position.set("fullCapacity", buf.readUnsignedShort());
            position.set("batteryHealth", buf.readUnsignedByte());
            position.set("batteryTemp", buf.readUnsignedShort() * 0.1 - 273.1);
            position.set("current", buf.readUnsignedShort());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
            position.set("cycleIndex", buf.readUnsignedShort());
            for (int i = 1; i <= 14; i++) {
                position.set("batteryCell" + i, buf.readUnsignedShort() * 0.001);
            }
            position.set("currentChargeInterval", buf.readUnsignedShort());
            position.set("maxChargeInterval", buf.readUnsignedShort());
            position.set("barcode", buf.readCharSequence(16, StandardCharsets.US_ASCII).toString().trim());
            position.set("batteryVersion", buf.readUnsignedShort());
            position.set("manufacturer", buf.readCharSequence(16, StandardCharsets.US_ASCII).toString().trim());
            position.set("batteryStatus", buf.readUnsignedInt());

            position.set("controllerStatus", buf.readUnsignedInt());
            position.set("controllerFault", buf.readUnsignedInt());

            sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);

            return position;

        } else if (type == MSG_STATUS && buf.readableBytes() == 22) {

            getLastLocation(position, null);

            buf.readUnsignedByte(); // information content
            buf.readUnsignedShort(); // satellites
            buf.readUnsignedByte(); // alarm
            buf.readUnsignedByte(); // language

            position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());

            buf.readUnsignedByte(); // working mode
            buf.readUnsignedShort(); // working voltage
            buf.readUnsignedByte(); // reserved
            buf.readUnsignedShort(); // working times
            buf.readUnsignedShort(); // working time

            int value = buf.readUnsignedShort();
            double temperature = BitUtil.to(value, 15) * 0.1;
            position.set(Position.PREFIX_TEMP + 1, BitUtil.check(value, 15) ? temperature : -temperature);

        } else if (isSupported(type)) {

            if (type == MSG_LBS_STATUS && variant == Variant.SPACE10X) {
                return null; // multi-lbs message
            }

            position.set(Position.KEY_TYPE, type);

            if (hasGps(type)) {
                decodeGps(position, buf, false, deviceSession.get(DeviceSession.KEY_TIMEZONE));
            } else {
                getLastLocation(position, null);
            }

            if (hasLbs(type) && buf.readableBytes() > 6) {
                boolean hasLength = hasStatus(type)
                        && type != MSG_LBS_STATUS
                        && type != MSG_LBS_ALARM
                        && (type != MSG_GPS_LBS_STATUS_1 || variant != Variant.VXT01)
                        && type != MSG_GPS_LBS_STATUS_5;
                decodeLbs(position, buf, type, hasLength);
            }

            if (hasStatus(type)) {
                if (type == MSG_GPS_LBS_STATUS_5) {
                    buf.readUnsignedByte(); // network indicator
                }
                decodeStatus(position, buf);
                if (variant == Variant.OBD6) {
                    int signal = buf.readUnsignedShort();
                    int satellites = BitUtil.between(signal, 10, 15) + BitUtil.between(signal, 5, 10);
                    position.set(Position.KEY_SATELLITES, satellites);
                    position.set(Position.KEY_RSSI, BitUtil.to(signal, 5));
                    position.addAlarm(decodeAlarm(buf.readUnsignedByte(), modelLW));
                    buf.readUnsignedByte(); // language
                    position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                    buf.readUnsignedByte(); // working mode
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() / 100.0);
                } else {
                    if (type == MSG_GPS_LBS_STATUS_5) {
                        position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    }
                    if (type == MSG_STATUS && "R11".equals(model)) {
                        position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    } else {
                        int battery = buf.readUnsignedByte();
                        if (battery <= 6) {
                            position.set(Position.KEY_BATTERY_LEVEL, battery * 100 / 6);
                        } else if (battery <= 100) {
                            position.set(Position.KEY_BATTERY_LEVEL, battery);
                        }
                    }
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    if (type == MSG_STATUS && modelLW) {
                        position.set(Position.KEY_POWER, BitUtil.to(buf.readUnsignedShort(), 12) / 10.0);
                    } else {
                        short extension = buf.readUnsignedByte();
                        if (type == MSG_STATUS && "SEEWORLD".equals(model)) {
                            position.set(Position.KEY_POWER, (double) extension);
                        } else if (variant != Variant.VXT01) {
                            position.addAlarm(decodeAlarm(extension, modelLW));
                        }
                    }
                }
            }

            if (type == MSG_STATUS_2) {
                buf.readUnsignedByte(); // language
                while (buf.readableBytes() > 6) {
                    int moduleType = buf.readUnsignedShort();
                    int moduleLength = buf.readUnsignedByte();
                    switch (moduleType) {
                        case 0x0018 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 100.0);
                        case 0x0032 -> position.set("startupStatus", buf.readUnsignedByte());
                        case 0x006A -> position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte());
                        default -> buf.skipBytes(moduleLength);
                    }
                }
            }

            if (type == MSG_GPS_LBS_STATUS_5) {
                buf.readUnsignedByte(); // language
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                position.set(Position.KEY_HOURS, buf.readUnsignedInt() * 1000);
                buf.skipBytes(8); // terminal id
                position.set(Position.KEY_INPUT, buf.readUnsignedShort());
            }

            if (type == MSG_GPS_LBS_1) {
                if (variant == Variant.GT06E_CARD) {
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    String data = buf.readCharSequence(buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString();
                    buf.readUnsignedByte(); // alarm
                    buf.readUnsignedByte(); // swiped
                    position.set(Position.KEY_CARD, data.trim());
                } else if (variant == Variant.BENWAY) {
                    int mask = buf.readUnsignedShort();
                    position.set(Position.KEY_IGNITION, BitUtil.check(mask, 8 + 7));
                    position.set(Position.PREFIX_IN + 2, BitUtil.check(mask, 8 + 6));
                    if (BitUtil.check(mask, 8 + 4)) {
                        int value = BitUtil.to(mask, 8 + 1);
                        if (BitUtil.check(mask, 8 + 1)) {
                            value = -value;
                        }
                        position.set(Position.PREFIX_TEMP + 1, value);
                    } else {
                        int value = BitUtil.to(mask, 8 + 2);
                        if (BitUtil.check(mask, 8 + 5)) {
                            position.set(Position.PREFIX_ADC + 1, value);
                        } else {
                            position.set(Position.PREFIX_ADC + 1, value * 0.1);
                        }
                    }
                } else if (variant == Variant.VXT01) {
                    decodeStatus(position, buf);
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    buf.readUnsignedByte(); // alarm extension
                } else if (variant == Variant.S5) {
                    decodeStatus(position, buf);
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    position.set(Position.KEY_RSSI, buf.readUnsignedByte());
                    position.addAlarm(decodeAlarm(buf.readUnsignedByte(), modelLW));
                    position.set("oil", buf.readUnsignedShort());
                    int temperature = buf.readUnsignedByte();
                    if (BitUtil.check(temperature, 7)) {
                        temperature = -BitUtil.to(temperature, 7);
                    }
                    position.set(Position.PREFIX_TEMP + 1, temperature);
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt() * 10);
                } else if (variant == Variant.WETRUST) {
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    position.set(Position.KEY_CARD, buf.readCharSequence(
                            buf.readUnsignedByte(), StandardCharsets.US_ASCII).toString());
                    position.addAlarm(buf.readUnsignedByte() > 0 ? Position.ALARM_GENERAL : null);
                    position.set("cardStatus", buf.readUnsignedByte());
                    position.set(Position.KEY_DRIVING_TIME, buf.readUnsignedShort());
                }
            }

            if (type == MSG_GPS_LBS_2 && variant == Variant.SEEWORLD) {
                position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
                buf.readUnsignedByte(); // reporting mode
                buf.readUnsignedByte(); // supplementary transmission
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                buf.readUnsignedInt(); // travel time
                int temperature = buf.readUnsignedShort();
                if (BitUtil.check(temperature, 15)) {
                    temperature = -BitUtil.to(temperature, 15);
                }
                position.set(Position.PREFIX_TEMP + 1, temperature * 0.01);
                position.set("humidity", buf.readUnsignedShort() * 0.01);
            }

            if (type == MSG_GPS_LBS_STATUS_4 && variant == Variant.SL4X) {
                position.setAltitude(buf.readShort());
            }

            if ((type == MSG_GPS_LBS_2 || type == MSG_GPS_LBS_3 || type == MSG_GPS_LBS_4 || type == MSG_GPS_LBS_5)
                    && buf.readableBytes() >= 3 + 6) {
                position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
                position.set(Position.KEY_EVENT, buf.readUnsignedByte()); // reason
                position.set(Position.KEY_ARCHIVE, buf.readUnsignedByte() > 0);
                if (variant == Variant.SL4X) {
                    if (buf.readableBytes() > 2 + 6) {
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    }
                    position.setAltitude(buf.readShort());
                }
            }

            if (type == MSG_GPS_LBS_3) {
                int module = buf.readUnsignedShort();
                int subLength = buf.readUnsignedByte();
                switch (module) {
                    case 0x0027 -> position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    case 0x002E -> position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    case 0x003B -> position.setAccuracy(buf.readUnsignedShort() * 0.01);
                    default -> buf.skipBytes(subLength);
                }
            }

            if (type == MSG_GPS_LBS_RFID) {
                position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(8)));
                buf.readUnsignedByte(); // validity
            }

            if (buf.readableBytes() == 3 + 6 || buf.readableBytes() == 3 + 4 + 6) {
                position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
                buf.readUnsignedByte(); // upload mode
                position.set(Position.KEY_ARCHIVE, buf.readUnsignedByte() > 0 ? true : null);
            }

            if (buf.readableBytes() == 4 + 6) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (type == MSG_GPS_LBS_STATUS_3 || type == MSG_FENCE_MULTI) {
                position.set(Position.KEY_GEOFENCE, buf.readUnsignedByte());
            }

        } else if (type == MSG_ALARM) {

            boolean extendedAlarm = dataLength > 7;
            if (extendedAlarm) {
                if (variant == Variant.JC400) {
                    buf.readUnsignedShort(); // marker
                    buf.readUnsignedByte(); // version
                }
                decodeGps(
                        position, buf, false,
                        variant == Variant.JC400, variant == Variant.JC400, variant == Variant.JC400,
                        deviceSession.get(DeviceSession.KEY_TIMEZONE));
            } else {
                DateBuilder dateBuilder = new DateBuilder((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE))
                        .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                getLastLocation(position, dateBuilder.getDate());
            }
            if (variant == Variant.JC400) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.1);
            }
            short event = buf.readUnsignedByte();
            position.set(Position.KEY_EVENT, event);
            switch (event) {
                case 0x01 -> position.addAlarm(extendedAlarm ? Position.ALARM_SOS : Position.ALARM_GENERAL);
                case 0x0E -> position.addAlarm(Position.ALARM_LOW_POWER);
                case 0x76 -> position.addAlarm(Position.ALARM_TEMPERATURE);
                case 0x80 -> position.addAlarm(Position.ALARM_VIBRATION);
                case 0x87 -> position.addAlarm(Position.ALARM_OVERSPEED);
                case 0x88 -> position.addAlarm(Position.ALARM_POWER_CUT);
                case 0x90 -> position.addAlarm(Position.ALARM_ACCELERATION);
                case 0x91 -> position.addAlarm(Position.ALARM_BRAKING);
                case 0x92 -> position.addAlarm(Position.ALARM_CORNERING);
                case 0x93 -> position.addAlarm(Position.ALARM_ACCIDENT);
            }

        } else {

            if (dataLength > 0) {
                buf.skipBytes(dataLength);
            }
            if (type != MSG_COMMAND_0 && type != MSG_COMMAND_1 && type != MSG_COMMAND_2) {
                sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);
            }
            return null;

        }

        sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);

        return position;
    }

    private Object decodeExtended(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (!deviceSession.contains(DeviceSession.KEY_TIMEZONE)) {
            deviceSession.set(DeviceSession.KEY_TIMEZONE, getTimeZone(deviceSession.getDeviceId()));
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedShort(); // length
        int type = buf.readUnsignedByte();

        if (type == MSG_STRING_INFO) {

            buf.readUnsignedInt(); // server flag
            String data;
            if (buf.readUnsignedByte() == 1) {
                data = buf.readSlice(buf.readableBytes() - 6).toString(StandardCharsets.US_ASCII);
            } else {
                data = buf.readSlice(buf.readableBytes() - 6).toString(StandardCharsets.UTF_16BE);
            }

            Parser parser = new Parser(PATTERN_LOCATION, data);

            if (parser.matches()) {
                position.setValid(true);
                position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
                position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
                position.setCourse(parser.nextDouble());
                position.setSpeed(parser.nextDouble());
                position.setTime(parser.nextDateTime(Parser.DateTimeFormat.YMD_HMS));
            } else {
                getLastLocation(position, null);
                position.set(Position.KEY_RESULT, data);
            }

            return position;

        } else if (type == MSG_INFO) {

            int subType = buf.readUnsignedByte();

            getLastLocation(position, null);

            if (subType == 0x00) {

                position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() * 0.01);
                return position;

            } else if (subType == 0x04) {

                CharSequence content = buf.readCharSequence(buf.readableBytes() - 4 - 2, StandardCharsets.US_ASCII);
                String[] values = content.toString().split(";");
                for (String value : values) {
                    String[] pair = value.split("=");
                    switch (pair[0]) {
                        case "ALM1":
                        case "ALM2":
                        case "ALM3":
                            position.set("alarm" + pair[0].charAt(3) + "Status", Integer.parseInt(pair[1], 16));
                        case "STA1":
                            position.set("otherStatus", Integer.parseInt(pair[1], 16));
                            break;
                        case "DYD":
                            position.set("engineStatus", Integer.parseInt(pair[1], 16));
                            break;
                        default:
                            break;
                    }
                }
                return position;

            } else if (subType == 0x05) {

                if (buf.readableBytes() >= 6 + 1 + 6) {
                    DateBuilder dateBuilder = new DateBuilder((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE))
                            .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                            .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                    position.setDeviceTime(dateBuilder.getDate());
                }

                int flags = buf.readUnsignedByte();
                position.set(Position.KEY_DOOR, BitUtil.check(flags, 0));
                position.set(Position.PREFIX_IO + 1, BitUtil.check(flags, 2));
                return position;

            } else if (subType == 0x0a) {

                buf.skipBytes(8); // imei
                buf.skipBytes(8); // imsi
                position.set(Position.KEY_ICCID, ByteBufUtil.hexDump(buf.readSlice(10)).replaceAll("f", ""));
                return position;

            } else if (subType == 0x0b) {

                position.set("networkTechnology", buf.readByte() > 0 ? "4G" : "2G");
                return position;

            } else if (subType == 0x0d) {

                if (buf.getByte(buf.readerIndex()) != '!') {
                    buf.skipBytes(6);
                }

                Parser parser = new Parser(PATTERN_FUEL, buf.toString(
                        buf.readerIndex(), buf.readableBytes() - 4 - 2, StandardCharsets.US_ASCII));
                if (!parser.matches()) {
                    return null;
                }

                position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
                position.set(Position.KEY_FUEL_LEVEL, parser.nextDouble(0));

                return position;

            } else if (subType == 0x1b) {

                if (Character.isLetter(buf.getUnsignedByte(buf.readerIndex()))) {
                    String data = buf.readCharSequence(buf.readableBytes() - 6, StandardCharsets.US_ASCII).toString();
                    position.set("serial", data.trim());
                } else {
                    buf.readUnsignedByte(); // header
                    buf.readUnsignedByte(); // type
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, ByteBufUtil.hexDump(buf.readSlice(4)));
                    buf.readUnsignedByte(); // checksum
                    buf.readUnsignedByte(); // footer
                }
                return position;

            }

        } else if (type == MSG_X1_PHOTO_DATA) {

            int pictureId = buf.readInt();

            ByteBuf photo = photos.get(pictureId);

            buf.readUnsignedInt(); // offset
            buf.readBytes(photo, buf.readUnsignedShort());

            if (photo.writableBytes() > 0) {
                sendPhotoRequest(channel, pictureId);
            } else {
                position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), photo, "jpg"));
                photos.remove(pictureId).release();
            }

        } else if (type == MSG_AZ735_GPS || type == MSG_AZ735_ALARM) {

            if (!decodeGps(position, buf, true, deviceSession.get(DeviceSession.KEY_TIMEZONE))) {
                getLastLocation(position, position.getDeviceTime());
            }

            if (decodeLbs(position, buf, type, true)) {
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            }

            buf.skipBytes(buf.readUnsignedByte()); // additional cell towers
            buf.skipBytes(buf.readUnsignedByte()); // wifi access point

            int status = buf.readUnsignedByte();
            position.set(Position.KEY_STATUS, status);

            if (type == MSG_AZ735_ALARM) {
                switch (status) {
                    case 0xA0 -> position.set(Position.KEY_ARMED, true);
                    case 0xA1 -> position.set(Position.KEY_ARMED, false);
                    case 0xA2, 0xA3 -> position.addAlarm(Position.ALARM_LOW_BATTERY);
                    case 0xA4 -> position.addAlarm(Position.ALARM_GENERAL);
                    case 0xA5 -> position.addAlarm(Position.ALARM_DOOR);
                }
            }

            buf.skipBytes(buf.readUnsignedByte()); // reserved extension

            sendResponse(channel, true, type, buf.getShort(buf.writerIndex() - 6), null);

            return position;

        } else if (type == MSG_OBD) {

            DateBuilder dateBuilder = new DateBuilder((TimeZone) deviceSession.get(DeviceSession.KEY_TIMEZONE))
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

            getLastLocation(position, dateBuilder.getDate());

            position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);

            String data = buf.readCharSequence(buf.readableBytes() - 18, StandardCharsets.US_ASCII).toString();
            for (String pair : data.split(",")) {
                String[] values = pair.split("=");
                if (values.length >= 2) {
                    switch (Integer.parseInt(values[0].substring(0, 2), 16)) {
                        case 40 -> position.set(Position.KEY_ODOMETER, Integer.parseInt(values[1], 16) * 0.01);
                        case 43 -> position.set(Position.KEY_FUEL_LEVEL, Integer.parseInt(values[1], 16) * 0.01);
                        case 45 -> position.set(Position.KEY_COOLANT_TEMP, Integer.parseInt(values[1], 16) * 0.01);
                        case 53 -> position.set(Position.KEY_OBD_SPEED, Integer.parseInt(values[1], 16) * 0.01);
                        case 54 -> position.set(Position.KEY_RPM, Integer.parseInt(values[1], 16) * 0.01);
                        case 71 -> position.set(Position.KEY_FUEL_USED, Integer.parseInt(values[1], 16) * 0.01);
                        case 73 -> position.set(Position.KEY_HOURS, Integer.parseInt(values[1], 16) * 0.01);
                        case 74 -> position.set(Position.KEY_VIN, values[1]);
                    }
                }
            }

            return position;

        } else if (type == MSG_GPS_MODULAR) {

            while (buf.readableBytes() > 6) {
                int moduleType = buf.readUnsignedShort();
                int moduleLength = buf.readUnsignedShort();

                switch (moduleType) {
                    case 0x03 -> position.set(Position.KEY_ICCID, ByteBufUtil.hexDump(buf.readSlice(10)));
                    case 0x09 -> position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                    case 0x0a -> position.set(Position.KEY_SATELLITES_VISIBLE, buf.readUnsignedByte());
                    case 0x11 -> {
                        CellTower cellTower = CellTower.from(
                                buf.readUnsignedShort(),
                                buf.readUnsignedShort(),
                                buf.readUnsignedShort(),
                                buf.readUnsignedMedium(),
                                buf.readUnsignedByte());
                        if (cellTower.getCellId() > 0) {
                            position.setNetwork(new Network(cellTower));
                        }
                    }
                    case 0x18 -> position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.01);
                    case 0x28 -> position.set(Position.KEY_HDOP, buf.readUnsignedByte() * 0.1);
                    case 0x29 -> position.set(Position.KEY_INDEX, buf.readUnsignedInt());
                    case 0x2a -> {
                        int input = buf.readUnsignedByte();
                        position.set(Position.KEY_DOOR, BitUtil.to(input, 4) > 0);
                        position.set("tamper", BitUtil.from(input, 4) > 0);
                    }
                    case 0x2b -> {
                        int event = buf.readUnsignedByte();
                        switch (event) {
                            case 0x11:
                                position.addAlarm(Position.ALARM_LOW_BATTERY);
                                break;
                            case 0x12:
                                position.addAlarm(Position.ALARM_LOW_POWER);
                                break;
                            case 0x13:
                                position.addAlarm(Position.ALARM_POWER_CUT);
                                break;
                            case 0x14:
                                position.addAlarm(Position.ALARM_REMOVING);
                                break;
                            default:
                                break;
                        }
                        position.set(Position.KEY_EVENT, event);
                    }
                    case 0x2e -> position.set(Position.KEY_ODOMETER, buf.readUnsignedIntLE());
                    case 0x33 -> {
                        position.setTime(new Date(buf.readUnsignedInt() * 1000));
                        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                        position.setAltitude(buf.readShort());

                        double latitude = buf.readUnsignedInt() / 60.0 / 30000.0;
                        double longitude = buf.readUnsignedInt() / 60.0 / 30000.0;
                        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

                        int flags = buf.readUnsignedShort();
                        position.setCourse(BitUtil.to(flags, 10));
                        position.setValid(BitUtil.check(flags, 12));

                        if (!BitUtil.check(flags, 10)) {
                            latitude = -latitude;
                        }
                        if (BitUtil.check(flags, 11)) {
                            longitude = -longitude;
                        }

                        position.setLatitude(latitude);
                        position.setLongitude(longitude);
                    }
                    case 0x34 -> {
                        position.set(Position.KEY_EVENT, buf.readUnsignedByte());
                        buf.readUnsignedIntLE(); // time
                        buf.skipBytes(buf.readUnsignedByte()); // content
                    }
                    default -> buf.skipBytes(moduleLength);
                }
            }

            if (position.getFixTime() == null) {
                getLastLocation(position, null);
            }

            sendResponse(channel, false, MSG_GPS_MODULAR, buf.readUnsignedShort(), null);

            return position;

        } else if (type == MSG_MULTIMEDIA) {

            buf.skipBytes(8); // serial number
            long timestamp = buf.readUnsignedInt() * 1000;
            buf.skipBytes(4 + 4 + 2 + 1 + 1 + 2); // gps
            buf.skipBytes(2 + 2 + 2 + 2); // cell

            int mediaId = buf.readInt();
            int mediaLength = buf.readInt();
            int mediaType = buf.readUnsignedByte();
            int mediaFormat = buf.readUnsignedByte();

            if (mediaType == 0 && mediaFormat == 0) {

                buf.readUnsignedByte(); // event

                ByteBuf photo;
                if (buf.readUnsignedShort() == 0) {
                    photo = Unpooled.buffer(mediaLength);
                    if (photos.containsKey(mediaId)) {
                        photos.remove(mediaId).release();
                    }
                    photos.put(mediaId, photo);
                } else {
                    photo = photos.get(mediaId);
                }

                if (photo != null) {
                    buf.readBytes(photo, buf.readableBytes() - 3 * 2);
                    if (!photo.isWritable()) {
                        position = new Position(getProtocolName());
                        position.setDeviceId(deviceSession.getDeviceId());
                        getLastLocation(position, new Date(timestamp));
                        position.set(Position.KEY_IMAGE, writeMediaFile(deviceSession.getUniqueId(), photo, "jpg"));
                        photos.remove(mediaId).release();
                    }
                }

            }

            sendResponse(channel, true, type, buf.getShort(buf.writerIndex() - 6), null);

            return position;

        } else if (type == MSG_SERIAL) {

            position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);

            buf.readUnsignedByte(); // external device type code

            ByteBuf data = buf.readSlice(buf.readableBytes() - 6); // index + checksum + footer
            if (BufferUtil.isPrintable(data, data.readableBytes())) {
                String value = data.readCharSequence(data.readableBytes(), StandardCharsets.US_ASCII).toString();
                position.set(Position.KEY_RESULT, value.trim());
            } else {
                position.set(Position.KEY_RESULT, ByteBufUtil.hexDump(data));
            }

            return position;

        }

        return null;
    }

    private void decodeVariant(ByteBuf buf) {
        int header = buf.getUnsignedShort(buf.readerIndex());
        int length;
        int type;
        if (header == 0x7878) {
            length = buf.getUnsignedByte(buf.readerIndex() + 2);
            type = buf.getUnsignedByte(buf.readerIndex() + 2 + 1);
        } else {
            length = buf.getUnsignedShort(buf.readerIndex() + 2);
            type = buf.getUnsignedByte(buf.readerIndex() + 2 + 2);
        }

        if (header == 0x7878 && type == MSG_GPS_LBS_1 && length == 0x24) {
            variant = Variant.VXT01;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_STATUS_1 && length == 0x24) {
            variant = Variant.VXT01;
        } else if (header == 0x7878 && type == MSG_LBS_MULTIPLE_3 && length == 0x31) {
            variant = Variant.WANWAY_S20;
        } else if (header == 0x7878 && type == MSG_LBS_MULTIPLE_3 && length == 0x2e) {
            variant = Variant.SR411_MINI;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_1 && length >= 0x71) {
            variant = Variant.GT06E_CARD;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_1 && length == 0x21) {
            variant = Variant.BENWAY;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_1 && length == 0x2b) {
            variant = Variant.S5;
        } else if (header == 0x7878 && type == MSG_LBS_STATUS && length >= 0x17) {
            variant = Variant.SPACE10X;
        } else if (header == 0x7878 && type == MSG_STATUS && length == 0x13) {
            variant = Variant.OBD6;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_1 && length == 0x29) {
            variant = Variant.WETRUST;
        } else if (header == 0x7878 && type == MSG_ALARM && buf.getUnsignedShort(buf.readerIndex() + 4) == 0xffff) {
            variant = Variant.JC400;
        } else if (header == 0x7878 && type == MSG_LBS_3 && length == 0x37) {
            variant = Variant.SL4X;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_5 && length == 0x2a) {
            variant = Variant.SL4X;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_STATUS_4 && length == 0x27) {
            variant = Variant.SL4X;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_STATUS_4 && length == 0x29) {
            variant = Variant.SL4X;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_2 && length == 0x2f) {
            variant = Variant.SEEWORLD;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_STATUS_1 && length == 0x26) {
            variant = Variant.SEEWORLD;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_RFID && length == 0x28) {
            variant = Variant.RFID;
        } else if (header == 0x7878 && type == MSG_GPS_LBS_STATUS_5 && length == 0x40) {
            variant = Variant.LW4G;
        } else {
            variant = Variant.STANDARD;
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        decodeVariant(buf);

        int header = buf.readShort();

        if (header == 0x7878) {
            return decodeBasic(channel, remoteAddress, buf);
        } else {
            return decodeExtended(channel, remoteAddress, buf);
        }
    }

}
