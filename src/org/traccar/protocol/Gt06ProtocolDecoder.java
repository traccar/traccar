/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.Checksum;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

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

    private String readImei(ChannelBuffer buf) {
        int b = buf.readUnsignedByte();
        StringBuilder imei = new StringBuilder();
        imei.append(b & 0x0F);
        for (int i = 0; i < 7; i++) {
            b = buf.readUnsignedByte();
            imei.append((b & 0xF0) >> 4);
            imei.append(b & 0x0F);
        }
        return imei.toString();
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

    private final static Set<Integer> MESSAGES_SUPPORTED = new HashSet<>(Arrays.asList(
            MSG_GPS,
            MSG_GPS_LBS_1,
            MSG_GPS_LBS_2,
            MSG_GPS_LBS_STATUS_1,
            MSG_GPS_LBS_STATUS_2,
            MSG_GPS_LBS_STATUS_3,
            MSG_GPS_PHONE,
            MSG_GPS_LBS_EXTEND));

    private final static Set<Integer> MESSAGES_LBS = new HashSet<>(Arrays.asList(
            MSG_GPS_LBS_1,
            MSG_GPS_LBS_2,
            MSG_GPS_LBS_STATUS_1,
            MSG_GPS_LBS_STATUS_2,
            MSG_GPS_LBS_STATUS_3));

    private final static Set<Integer> MESSAGES_STATUS = new HashSet<>(Arrays.asList(
            MSG_GPS_LBS_STATUS_1,
            MSG_GPS_LBS_STATUS_2,
            MSG_GPS_LBS_STATUS_3));

    private static void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(10);
            response.writeByte(0x78); response.writeByte(0x78); // header
            response.writeByte(0x05); // size
            response.writeByte(type);
            response.writeShort(index);
            response.writeShort(Checksum.crc16(Checksum.CRC16_X25, response.toByteBuffer(2, 4)));
            response.writeByte(0x0D); response.writeByte(0x0A); // ending
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.readByte() != 0x78 || buf.readByte() != 0x78) {
            return null;
        }

        int length = buf.readUnsignedByte(); // size
        int dataLength = length - 5;

        int type = buf.readUnsignedByte();

        if (type == MSG_LOGIN) {

            String imei = readImei(buf);
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
                    timeZone.setRawOffset(offset);
                }
            }

            if (identify(imei, channel)) {
                buf.skipBytes(buf.readableBytes() - 6);
                sendResponse(channel, type, buf.readUnsignedShort());
            }

        } else if (hasDeviceId()) {

            if (MESSAGES_SUPPORTED.contains(type)) {

                Position position = new Position();
                position.setDeviceId(getDeviceId());
                position.setProtocol(getProtocolName());

                DateBuilder dateBuilder = new DateBuilder(timeZone)
                        .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                position.setTime(dateBuilder.getDate());

                // GPS length and Satellites count
                int gpsLength = buf.readUnsignedByte();
                position.set(Event.KEY_SATELLITES, gpsLength & 0b0000_1111);
                gpsLength >>= 4;

                // Latitude
                double latitude = buf.readUnsignedInt() / (60.0 * 30000.0);

                // Longitude
                double longitude = buf.readUnsignedInt() / (60.0 * 30000.0);

                // Speed
                position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

                // Course and flags
                int union = buf.readUnsignedShort();
                position.setCourse(union & 0b0000_0011_1111_1111);
                position.setValid((union & 0b0001_0000_0000_0000) != 0);
                if ((union & 0b0000_0100_0000_0000) == 0) latitude = -latitude;
                if ((union & 0b0000_1000_0000_0000) != 0) longitude = -longitude;

                position.setLatitude(latitude);
                position.setLongitude(longitude);

                if ((union & 0b0100_0000_0000_0000) != 0) {
                    position.set(Event.KEY_IGNITION, (union & 0b1000_0000_0000_0000) != 0);
                }

                buf.skipBytes(gpsLength - 12); // skip reserved

                if (MESSAGES_LBS.contains(type)) {

                    int lbsLength = 0;
                    if (MESSAGES_STATUS.contains(type)) {
                        lbsLength = buf.readUnsignedByte();
                    }

                    // Cell information
                    position.set(Event.KEY_MCC, buf.readUnsignedShort());
                    position.set(Event.KEY_MNC, buf.readUnsignedByte());
                    position.set(Event.KEY_LAC, buf.readUnsignedShort());
                    position.set(Event.KEY_CELL, (buf.readUnsignedShort() << 8) + buf.readUnsignedByte());
                    if (lbsLength > 0) {
                        buf.skipBytes(lbsLength - 9);
                    }

                    if (MESSAGES_STATUS.contains(type)) {
                        position.set(Event.KEY_ALARM, true);

                        int flags = buf.readUnsignedByte();

                        position.set(Event.KEY_IGNITION, (flags & 0x2) != 0);
                        // TODO parse other flags

                        // Voltage
                        position.set(Event.KEY_POWER, buf.readUnsignedByte());

                        // GSM signal
                        position.set(Event.KEY_GSM, buf.readUnsignedByte());
                    }

                }

                if (type == MSG_GPS_LBS_1 && buf.readableBytes() == 4 + 6) {
                    position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
                }

                // Index
                if (buf.readableBytes() > 6) {
                    buf.skipBytes(buf.readableBytes() - 6);
                }
                int index = buf.readUnsignedShort();
                position.set(Event.KEY_INDEX, index);
                sendResponse(channel, type, index);
                return position;

            } else {

                buf.skipBytes(dataLength);
                if (type != MSG_COMMAND_0 && type != MSG_COMMAND_1 && type != MSG_COMMAND_2) {
                    sendResponse(channel, type, buf.readUnsignedShort());
                }

            }

        }

        return null;
    }

}
