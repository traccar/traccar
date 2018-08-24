/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Device;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Gt06ProtocolDecoder extends BaseProtocolDecoder {

    private final Map<Integer, ByteBuf> photos = new HashMap<>();

    public Gt06ProtocolDecoder(Gt06Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x01;
    public static final int MSG_GPS = 0x10;
    public static final int MSG_LBS = 0x11;
    public static final int MSG_GPS_LBS_1 = 0x12;
    public static final int MSG_GPS_LBS_2 = 0x22;
    public static final int MSG_STATUS = 0x13;
    public static final int MSG_SATELLITE = 0x14;
    public static final int MSG_STRING = 0x15;
    public static final int MSG_GPS_LBS_STATUS_1 = 0x16;
    public static final int MSG_WIFI = 0x17;
    public static final int MSG_GPS_LBS_STATUS_2 = 0x26;
    public static final int MSG_GPS_LBS_STATUS_3 = 0x27;
    public static final int MSG_LBS_MULTIPLE = 0x28;
    public static final int MSG_LBS_WIFI = 0x2C;
    public static final int MSG_LBS_PHONE = 0x17;
    public static final int MSG_LBS_EXTEND = 0x18;
    public static final int MSG_LBS_STATUS = 0x19;
    public static final int MSG_GPS_PHONE = 0x1A;
    public static final int MSG_GPS_LBS_EXTEND = 0x1E;
    public static final int MSG_HEARTBEAT = 0x23;
    public static final int MSG_ADDRESS_REQUEST = 0x2A;
    public static final int MSG_ADDRESS_RESPONSE = 0x97;
    public static final int MSG_AZ735_GPS = 0x32;
    public static final int MSG_AZ735_ALARM = 0x33;
    public static final int MSG_X1_GPS = 0x34;
    public static final int MSG_X1_PHOTO_INFO = 0x35;
    public static final int MSG_X1_PHOTO_DATA = 0x36;
    public static final int MSG_WIFI_2 = 0x69;
    public static final int MSG_COMMAND_0 = 0x80;
    public static final int MSG_COMMAND_1 = 0x81;
    public static final int MSG_COMMAND_2 = 0x82;
    public static final int MSG_TIME_REQUEST = 0x8A;
    public static final int MSG_INFO = 0x94;
    public static final int MSG_STRING_INFO = 0x21;
    public static final int MSG_GPS_2 = 0xA0;
    public static final int MSG_LBS_2 = 0xA1;
    public static final int MSG_WIFI_3 = 0xA2;
    public static final int MSG_FENCE_SINGLE = 0xA3;
    public static final int MSG_FENCE_MULTI = 0xA4;
    public static final int MSG_LBS_ALARM = 0xA5;
    public static final int MSG_LBS_ADDRESS = 0xA7;

    private static boolean isSupported(int type) {
        return hasGps(type) || hasLbs(type) || hasStatus(type);
    }

    private static boolean hasGps(int type) {
        switch (type) {
            case MSG_GPS:
            case MSG_GPS_LBS_1:
            case MSG_GPS_LBS_2:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_GPS_PHONE:
            case MSG_GPS_LBS_EXTEND:
            case MSG_GPS_2:
            case MSG_FENCE_SINGLE:
            case MSG_FENCE_MULTI:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasLbs(int type) {
        switch (type) {
            case MSG_LBS:
            case MSG_LBS_STATUS:
            case MSG_GPS_LBS_1:
            case MSG_GPS_LBS_2:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_GPS_2:
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
            case MSG_LBS_STATUS:
            case MSG_GPS_LBS_STATUS_1:
            case MSG_GPS_LBS_STATUS_2:
            case MSG_GPS_LBS_STATUS_3:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasLanguage(int type) {
        switch (type) {
            case MSG_GPS_PHONE:
            case MSG_HEARTBEAT:
            case MSG_GPS_LBS_STATUS_3:
            case MSG_LBS_MULTIPLE:
            case MSG_LBS_2:
            case MSG_FENCE_MULTI:
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
            response.writeByte('\r'); response.writeByte('\n'); // ending
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

    private boolean decodeGps(Position position, ByteBuf buf, boolean hasLength, TimeZone timezone) {

        DateBuilder dateBuilder = new DateBuilder(timezone)
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        if (hasLength && buf.readUnsignedByte() == 0) {
            return false;
        }

        position.set(Position.KEY_SATELLITES, BitUtil.to(buf.readUnsignedByte(), 4));

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

        if (BitUtil.check(flags, 14)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(flags, 15));
        }

        return true;
    }

    private boolean decodeLbs(Position position, ByteBuf buf, boolean hasLength) {

        int length = 0;
        if (hasLength) {
            length = buf.readUnsignedByte();
            if (length == 0) {
                return false;
            }
        }

        int mcc = buf.readUnsignedShort();
        int mnc = BitUtil.check(mcc, 15) ? buf.readUnsignedShort() : buf.readUnsignedByte();

        position.setNetwork(new Network(CellTower.from(
                BitUtil.to(mcc, 15), mnc, buf.readUnsignedShort(), buf.readUnsignedMedium())));

        if (length > 9) {
            buf.skipBytes(length - 9);
        }

        return true;
    }

    private boolean decodeStatus(Position position, ByteBuf buf) {

        int status = buf.readUnsignedByte();

        position.set(Position.KEY_STATUS, status);
        position.set(Position.KEY_IGNITION, BitUtil.check(status, 1));
        position.set(Position.KEY_CHARGE, BitUtil.check(status, 2));
        position.set(Position.KEY_BLOCKED, BitUtil.check(status, 7));

        switch (BitUtil.between(status, 3, 6)) {
            case 1:
                position.set(Position.KEY_ALARM, Position.ALARM_SHOCK);
                break;
            case 2:
                position.set(Position.KEY_ALARM, Position.ALARM_POWER_CUT);
                break;
            case 3:
                position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                break;
            case 4:
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            default:
                break;
        }

        position.set(Position.KEY_BATTERY_LEVEL, buf.readUnsignedByte() * 100 / 6);
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));

        return true;
    }

    private String decodeAlarm(short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x02:
                return Position.ALARM_POWER_CUT;
            case 0x03:
            case 0x09:
                return Position.ALARM_VIBRATION;
            case 0x04:
                return Position.ALARM_GEOFENCE_ENTER;
            case 0x05:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x06:
                return Position.ALARM_OVERSPEED;
            case 0x0E:
            case 0x0F:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_POWER_OFF;
            default:
                return null;
        }
    }

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

    private Position decodeFuelData(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_FUEL, sentence);
        if (!parser.matches()) {
            return null;
        }

        position.set(Position.PREFIX_TEMP + 1, parser.nextDouble(0));
        position.set(Position.KEY_FUEL_LEVEL, parser.nextDouble(0));

        return position;
    }

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

    private Position decodeLocationString(Position position, String sentence) {
        Parser parser = new Parser(PATTERN_LOCATION, sentence);
        if (!parser.matches()) {
            return null;
        }

        position.setValid(true);
        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.HEM_DEG));
        position.setCourse(parser.nextDouble());
        position.setSpeed(parser.nextDouble());
        position.setTime(parser.nextDateTime(Parser.DateTimeFormat.YMD_HMS));

        return position;
    }

    private Object decodeBasic(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        int length = buf.readUnsignedByte();
        int dataLength = length - 5;
        int type = buf.readUnsignedByte();

        DeviceSession deviceSession = null;
        if (type != MSG_LOGIN) {
            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }
            if (deviceSession.getTimeZone() == null) {
                deviceSession.setTimeZone(getTimeZone(deviceSession.getDeviceId()));
            }
        }

        if (type == MSG_LOGIN) {

            String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
            buf.readUnsignedShort(); // type

            deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession != null && deviceSession.getTimeZone() == null) {
                deviceSession.setTimeZone(getTimeZone(deviceSession.getDeviceId()));
            }

            if (dataLength > 10) {
                int extensionBits = buf.readUnsignedShort();
                int hours = (extensionBits >> 4) / 100;
                int minutes = (extensionBits >> 4) % 100;
                int offset = (hours * 60 + minutes) * 60;
                if ((extensionBits & 0x8) != 0) {
                    offset = -offset;
                }
                if (deviceSession != null) {
                    TimeZone timeZone = deviceSession.getTimeZone();
                    if (timeZone.getRawOffset() == 0) {
                        timeZone.setRawOffset(offset * 1000);
                        deviceSession.setTimeZone(timeZone);
                    }
                }

            }

            if (deviceSession != null) {
                sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);
            }

        } else if (type == MSG_HEARTBEAT) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

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

        } else if (type == MSG_X1_GPS) {

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            buf.readUnsignedInt(); // data and alarm

            decodeGps(position, buf, false, deviceSession.getTimeZone());

            buf.readUnsignedShort(); // terminal info

            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

            position.setNetwork(new Network(CellTower.from(
                    buf.readUnsignedShort(), buf.readUnsignedByte(),
                    buf.readUnsignedShort(), buf.readUnsignedInt())));

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

        } else if (type == MSG_WIFI || type == MSG_WIFI_2) {

            return decodeWifi(buf, deviceSession);

        } else {

            return decodeBasicOther(channel, buf, deviceSession, type, dataLength);

        }

        return null;
    }

    private Object decodeWifi(ByteBuf buf, DeviceSession deviceSession) throws Exception {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2));
        getLastLocation(position, dateBuilder.getDate());

        Network network = new Network();

        int wifiCount = buf.getByte(2);
        for (int i = 0; i < wifiCount; i++) {
            String mac = String.format("%02x:%02x:%02x:%02x:%02x:%02x",
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
            network.addWifiAccessPoint(WifiAccessPoint.from(mac, buf.readUnsignedByte()));
        }

        int cellCount = buf.readUnsignedByte();
        int mcc = buf.readUnsignedShort();
        int mnc = buf.readUnsignedByte();
        for (int i = 0; i < cellCount; i++) {
            network.addCellTower(CellTower.from(
                    mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedShort(), buf.readUnsignedByte()));
        }

        position.setNetwork(network);

        return position;
    }

    private Object decodeBasicOther(Channel channel, ByteBuf buf,
            DeviceSession deviceSession, int type, int dataLength) throws Exception {

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        if (type == MSG_LBS_MULTIPLE || type == MSG_LBS_EXTEND || type == MSG_LBS_WIFI
                || type == MSG_LBS_2 || type == MSG_WIFI_3) {

            boolean longFormat = type == MSG_LBS_2 || type == MSG_WIFI_3;

            DateBuilder dateBuilder = new DateBuilder(deviceSession.getTimeZone())
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

            getLastLocation(position, dateBuilder.getDate());

            int mcc = buf.readUnsignedShort();
            int mnc = BitUtil.check(mcc, 15) ? buf.readUnsignedShort() : buf.readUnsignedByte();
            Network network = new Network();
            for (int i = 0; i < 7; i++) {
                int lac = longFormat ? buf.readInt() : buf.readUnsignedShort();
                int cid = longFormat ? (int) buf.readLong() : buf.readUnsignedMedium();
                int rssi = -buf.readUnsignedByte();
                if (lac > 0) {
                    network.addCellTower(CellTower.from(BitUtil.to(mcc, 15), mnc, lac, cid, rssi));
                }
            }

            buf.readUnsignedByte(); // time leads

            if (type != MSG_LBS_MULTIPLE && type != MSG_LBS_2) {
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
                buf.readUnsignedByte(); // server flag (reserved)
                position.set(Position.KEY_RESULT,
                        buf.readSlice(commandLength - 1).toString(StandardCharsets.US_ASCII));
            }

        } else if (isSupported(type)) {

            if (hasGps(type)) {
                decodeGps(position, buf, false, deviceSession.getTimeZone());
            } else {
                getLastLocation(position, null);
            }

            if (hasLbs(type)) {
                decodeLbs(position, buf, hasStatus(type));
            }

            if (hasStatus(type)) {
                decodeStatus(position, buf);
            }

            if (type == MSG_GPS_LBS_1 && buf.readableBytes() >= 4 + 6) {
                position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            }

            if (type == MSG_GPS_LBS_2 && buf.readableBytes() >= 3 + 6) {
                position.set(Position.KEY_IGNITION, buf.readUnsignedByte() > 0);
                position.set(Position.KEY_EVENT, buf.readUnsignedByte()); // reason
                position.set(Position.KEY_ARCHIVE, buf.readUnsignedByte() > 0);
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

        if (hasLanguage(type)) {
            buf.readUnsignedShort();
        }

        if (type == MSG_GPS_LBS_STATUS_3 || type == MSG_FENCE_MULTI) {
            position.set(Position.KEY_GEOFENCE, buf.readUnsignedByte());
        }

        sendResponse(channel, false, type, buf.getShort(buf.writerIndex() - 6), null);

        return position;
    }

    private Object decodeExtended(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        if (deviceSession.getTimeZone() == null) {
            deviceSession.setTimeZone(getTimeZone(deviceSession.getDeviceId()));
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

            if (decodeLocationString(position, data) == null) {
                getLastLocation(position, null);
                position.set(Position.KEY_RESULT, data);
            }

            return position;

        } else if (type == MSG_INFO) {

            int subType = buf.readUnsignedByte();

            getLastLocation(position, null);

            if (subType == 0x00) {

                position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                return position;

            } else if (subType == 0x05) {

                int flags = buf.readUnsignedByte();
                position.set(Position.KEY_DOOR, BitUtil.check(flags, 0));
                position.set(Position.PREFIX_IO + 1, BitUtil.check(flags, 2));
                return position;

            } else if (subType == 0x0d) {

                buf.skipBytes(6);
                return decodeFuelData(position, buf.toString(
                        buf.readerIndex(), buf.readableBytes() - 4 - 2, StandardCharsets.US_ASCII));

            }

        } else if (type == MSG_X1_PHOTO_DATA) {

            int pictureId = buf.readInt();

            ByteBuf photo = photos.get(pictureId);

            buf.readUnsignedInt(); // offset
            buf.readBytes(photo, buf.readUnsignedShort());

            if (photo.writableBytes() > 0) {
                sendPhotoRequest(channel, pictureId);
            } else {
                Device device = Context.getDeviceManager().getById(deviceSession.getDeviceId());
                position.set(
                        Position.KEY_IMAGE, Context.getMediaManager().writeFile(device.getUniqueId(), photo, "jpg"));
                photos.remove(pictureId).release();
            }

        } else if (type == MSG_AZ735_GPS || type == MSG_AZ735_ALARM) {

            if (!decodeGps(position, buf, true, deviceSession.getTimeZone())) {
                getLastLocation(position, position.getDeviceTime());
            }

            if (decodeLbs(position, buf, true)) {
                position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            }

            buf.skipBytes(buf.readUnsignedByte()); // additional cell towers
            buf.skipBytes(buf.readUnsignedByte()); // wifi access point

            int status = buf.readUnsignedByte();
            position.set(Position.KEY_STATUS, status);

            if (type == MSG_AZ735_ALARM) {
                switch (status) {
                    case 0xA0:
                        position.set(Position.KEY_ARMED, true);
                        break;
                    case 0xA1:
                        position.set(Position.KEY_ARMED, false);
                        break;
                    case 0xA2:
                    case 0xA3:
                        position.set(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY);
                        break;
                    case 0xA4:
                        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                        break;
                    case 0xA5:
                        position.set(Position.KEY_ALARM, Position.ALARM_DOOR);
                        break;
                    default:
                        break;
                }
            }

            buf.skipBytes(buf.readUnsignedByte()); // reserved extension

            sendResponse(channel, true, type, buf.getShort(buf.writerIndex() - 6), null);

            return position;

        }

        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int header = buf.readShort();

        if (header == 0x7878) {
            return decodeBasic(channel, remoteAddress, buf);
        } else if (header == 0x7979) {
            return decodeExtended(channel, remoteAddress, buf);
        }

        return null;
    }

}
