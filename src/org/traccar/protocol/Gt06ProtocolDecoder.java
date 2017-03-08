/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

public class Gt06ProtocolDecoder extends BaseProtocolDecoder {

    private boolean forceTimeZone = false;
    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");

    public Gt06ProtocolDecoder(Gt06Protocol protocol) {
        super(protocol);

        if (Context.getConfig().hasKey(getProtocolName() + ".timezone")) {
            forceTimeZone = true;
            timeZone.setRawOffset(Context.getConfig().getInteger(getProtocolName() + ".timezone") * 1000);
        }
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
    public static final int MSG_GPS_LBS_STATUS_2 = 0x26;
    public static final int MSG_GPS_LBS_STATUS_3 = 0x27;
    public static final int MSG_LBS_PHONE = 0x17;
    public static final int MSG_LBS_EXTEND = 0x18;
    public static final int MSG_LBS_STATUS = 0x19;
    public static final int MSG_GPS_PHONE = 0x1A;
    public static final int MSG_GPS_LBS_EXTEND = 0x1E;
    public static final int MSG_COMMAND_0 = 0x80;
    public static final int MSG_COMMAND_1 = 0x81;
    public static final int MSG_COMMAND_2 = 0x82;
    public static final int MSG_INFO = 0x94;

    private static boolean isSupported(int type) {
        return hasGps(type) || hasLbs(type) || hasStatus(type);
    }

    private static boolean hasGps(int type) {
        return type == MSG_GPS || type == MSG_GPS_LBS_1 || type == MSG_GPS_LBS_2
                || type == MSG_GPS_LBS_STATUS_1 || type == MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3
                || type == MSG_GPS_PHONE || type == MSG_GPS_LBS_EXTEND;
    }

    private static boolean hasLbs(int type) {
        return type == MSG_LBS || type == MSG_LBS_STATUS || type == MSG_GPS_LBS_1 || type == MSG_GPS_LBS_2
                || type == MSG_GPS_LBS_STATUS_1 || type ==  MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3;
    }

    private static boolean hasStatus(int type) {
        return type == MSG_STATUS || type == MSG_LBS_STATUS
                || type == MSG_GPS_LBS_STATUS_1 || type == MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3;
    }

    private static void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.dynamicBuffer();
            response.writeByte(0x78); response.writeByte(0x78); // header
            response.writeByte(5); // size
            response.writeByte(type);
            response.writeShort(index);
            response.writeShort(Checksum.crc16(Checksum.CRC16_X25, response.toByteBuffer(2, 4)));
            response.writeByte(0x0D); response.writeByte(0x0A); // ending
            channel.write(response);
        }
    }

    private void decodeGps(Position position, ChannelBuffer buf) {

        DateBuilder dateBuilder = new DateBuilder(timeZone)
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        int length = buf.readUnsignedByte();
        position.set(Position.KEY_SATELLITES, BitUtil.to(length, 4));
        length = BitUtil.from(length, 4);

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

        buf.skipBytes(length - 12); // skip reserved
    }

    private void decodeLbs(Position position, ChannelBuffer buf, boolean hasLength) {

        int lbsLength = 0;
        if (hasLength) {
            lbsLength = buf.readUnsignedByte();
        }

        position.setNetwork(new Network(CellTower.from(
                buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedShort(), buf.readUnsignedMedium())));

        if (lbsLength > 0) {
            buf.skipBytes(lbsLength - 9);
        }
    }

    private void decodeStatus(Position position, ChannelBuffer buf) {

        int flags = buf.readUnsignedByte();

        position.set(Position.KEY_IGNITION, BitUtil.check(flags, 1));
        position.set(Position.KEY_STATUS, flags);
        position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
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
                break;
        }
        return null;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        int header = buf.readShort();

        if (header == 0x7878) {

            int length = buf.readUnsignedByte();
            int dataLength = length - 5;
            int type = buf.readUnsignedByte();

            if (type == MSG_LOGIN) {

                String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
                buf.readUnsignedShort(); // type

                // Timezone offset
                if (dataLength > 10) {
                    int extensionBits = buf.readUnsignedShort();
                    int hours = (extensionBits >> 4) / 100;
                    int minutes = (extensionBits >> 4) % 100;
                    int offset = (hours * 60 + minutes) * 60;
                    if ((extensionBits & 0x8) != 0) {
                        offset = -offset;
                    }
                    if (!forceTimeZone) {
                        timeZone.setRawOffset(offset * 1000);
                    }
                }

                if (getDeviceSession(channel, remoteAddress, imei) != null) {
                    buf.skipBytes(buf.readableBytes() - 6);
                    sendResponse(channel, type, buf.readUnsignedShort());
                }

            } else {

                DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
                if (deviceSession == null) {
                    return null;
                }

                Position position = new Position();
                position.setDeviceId(deviceSession.getDeviceId());
                position.setProtocol(getProtocolName());

                if (type == MSG_LBS_EXTEND) {

                    DateBuilder dateBuilder = new DateBuilder(timeZone)
                            .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                            .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());

                    getLastLocation(position, dateBuilder.getDate());

                    int mcc = buf.readUnsignedShort();
                    int mnc = buf.readUnsignedByte();

                    Network network = new Network();
                    for (int i = 0; i < 7; i++) {
                        network.addCellTower(CellTower.from(
                                mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedMedium(), -buf.readUnsignedByte()));
                    }
                    position.setNetwork(network);

                } else if (type == MSG_STRING) {

                    getLastLocation(position, null);

                    int commandLength = buf.readUnsignedByte();

                    if (commandLength > 0) {
                        buf.readUnsignedByte(); // server flag (reserved)
                        position.set(Position.KEY_COMMAND,
                                buf.readBytes(commandLength - 1).toString(StandardCharsets.US_ASCII));
                    }

                } else if (isSupported(type)) {

                    if (hasGps(type)) {
                        decodeGps(position, buf);
                    } else {
                        getLastLocation(position, null);
                    }

                    if (hasLbs(type)) {
                        decodeLbs(position, buf, hasStatus(type));
                    }

                    if (hasStatus(type)) {
                        decodeStatus(position, buf);
                    }

                    if (type == MSG_GPS_LBS_1 && buf.readableBytes() == 4 + 6) {
                        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    }

                } else {

                    buf.skipBytes(dataLength);
                    if (type != MSG_COMMAND_0 && type != MSG_COMMAND_1 && type != MSG_COMMAND_2) {
                        sendResponse(channel, type, buf.readUnsignedShort());
                    }
                    return null;

                }

                if (buf.readableBytes() > 6) {
                    buf.skipBytes(buf.readableBytes() - 6);
                }
                sendResponse(channel, type, buf.readUnsignedShort());

                return position;

            }

        } else if (header == 0x7979) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            buf.readUnsignedShort(); // length
            int type = buf.readUnsignedByte();

            if (type == MSG_INFO) {
                int subType = buf.readUnsignedByte();

                Position position = new Position();
                position.setDeviceId(deviceSession.getDeviceId());
                position.setProtocol(getProtocolName());

                getLastLocation(position, null);

                if (subType == 0x00) {
                    position.set(Position.KEY_POWER, buf.readUnsignedShort() * 0.01);
                    return position;
                } else if (subType == 0x05) {
                    int flags = buf.readUnsignedByte();
                    position.set("door", BitUtil.check(flags, 0));
                    position.set(Position.PREFIX_IO + 1, BitUtil.check(flags, 2));
                    return position;
                }
            }

        }

        return null;
    }

}
