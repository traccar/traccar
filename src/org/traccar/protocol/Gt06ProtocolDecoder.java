/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.Crc;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

public class Gt06ProtocolDecoder extends BaseProtocolDecoder {

    private boolean forceTimeZone = false;
    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");

    public Gt06ProtocolDecoder(String protocol) {
        super(protocol);

        Properties properties = Context.getProps();
        if (properties != null && properties.containsKey(protocol + ".timezone")) {
            forceTimeZone = true;
            timeZone.setRawOffset(Integer.valueOf(properties.getProperty(protocol + ".timezone")) * 1000);
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

    private static final int MSG_LOGIN = 0x01;
    private static final int MSG_GPS = 0x10;
    private static final int MSG_LBS = 0x11;
    private static final int MSG_GPS_LBS_1 = 0x12;
    private static final int MSG_GPS_LBS_2 = 0x22;
    private static final int MSG_STATUS = 0x13;
    private static final int MSG_SATELLITE = 0x14;
    private static final int MSG_STRING = 0x15;
    private static final int MSG_GPS_LBS_STATUS_1 = 0x16;
    private static final int MSG_GPS_LBS_STATUS_2 = 0x26;
    private static final int MSG_GPS_LBS_STATUS_3 = 0x27;
    private static final int MSG_LBS_PHONE = 0x17;
    private static final int MSG_LBS_EXTEND = 0x18;
    private static final int MSG_LBS_STATUS = 0x19;
    private static final int MSG_GPS_PHONE = 0x1A;
    private static final int MSG_GPS_LBS_EXTEND = 0x1E;
    private static final int MSG_COMMAND_0 = 0x80;
    private static final int MSG_COMMAND_1 = 0x81;
    private static final int MSG_COMMAND_2 = 0x82;

    private static void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(10);
            response.writeByte(0x78); response.writeByte(0x78); // header
            response.writeByte(0x05); // size
            response.writeByte(type);
            response.writeShort(index);
            response.writeShort(Crc.crc16Ccitt(response.toByteBuffer(2, 4)));
            response.writeByte(0x0D); response.writeByte(0x0A); // ending
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Check header
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

            if (identify(imei)) {
                buf.skipBytes(buf.readableBytes() - 6);
                sendResponse(channel, type, buf.readUnsignedShort());
            }

        } else if (hasDeviceId() && (
                type == MSG_GPS ||
                type == MSG_GPS_LBS_1 ||
                type == MSG_GPS_LBS_2 ||
                type == MSG_GPS_LBS_STATUS_1 ||
                type == MSG_GPS_LBS_STATUS_2 ||
                type == MSG_GPS_LBS_STATUS_3 ||
                type == MSG_GPS_PHONE ||
                type == MSG_GPS_LBS_EXTEND)) {

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());

            // Date and time
            Calendar time = Calendar.getInstance(timeZone);
            time.clear();
            time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
            time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
            time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
            time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
            time.set(Calendar.MINUTE, buf.readUnsignedByte());
            time.set(Calendar.SECOND, buf.readUnsignedByte());
            position.setTime(time.getTime());

            // GPS length and Satellites count
            int gpsLength = buf.readUnsignedByte();
            position.set(Event.KEY_SATELLITES, gpsLength & 0xf);
            gpsLength >>= 4;

            // Latitude
            double latitude = buf.readUnsignedInt() / (60.0 * 30000.0);

            // Longitude
            double longitude = buf.readUnsignedInt() / (60.0 * 30000.0);

            // Speed
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

            // Course and flags
            int union = buf.readUnsignedShort();
            position.setCourse(union & 0x03FF);
            position.setValid((union & 0x1000) != 0);
            if ((union & 0x0400) == 0) latitude = -latitude;
            if ((union & 0x0800) != 0) longitude = -longitude;

            position.setLatitude(latitude);
            position.setLongitude(longitude);
            
            if ((union & 0x4000) != 0) {
                position.set("acc", (union & 0x8000) != 0);
            }

            buf.skipBytes(gpsLength - 12); // skip reserved

            if (type == MSG_GPS_LBS_1 || type == MSG_GPS_LBS_2 ||
                type == MSG_GPS_LBS_STATUS_1 || type == MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3) {

                int lbsLength = 0;
                if (type == MSG_GPS_LBS_STATUS_1 || type == MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3) {
                    lbsLength = buf.readUnsignedByte();
                }

                // Cell information
                position.set(Event.KEY_MCC, buf.readUnsignedShort());
                position.set(Event.KEY_MNC, buf.readUnsignedByte());
                position.set(Event.KEY_LAC, buf.readUnsignedShort());
                position.set(Event.KEY_CELL, buf.readUnsignedShort() << 8 + buf.readUnsignedByte());
                buf.skipBytes(lbsLength - 9);

                // Status
                if (type == MSG_GPS_LBS_STATUS_1 || type == MSG_GPS_LBS_STATUS_2 || type == MSG_GPS_LBS_STATUS_3) {

                    position.set(Event.KEY_ALARM, true);

                    int flags = buf.readUnsignedByte();

                    position.set("acc", (flags & 0x2) != 0);
                    // TODO parse other flags

                    // Voltage
                    position.set(Event.KEY_POWER, buf.readUnsignedByte());

                    // GSM signal
                    position.set(Event.KEY_GSM, buf.readUnsignedByte());
                }
            }

            // Index
            if (buf.readableBytes() > 6) {
                buf.skipBytes(buf.readableBytes() - 6);
            }
            int index = buf.readUnsignedShort();
            position.set(Event.KEY_INDEX, index);
            sendResponse(channel, type, index);
            return position;
        }
        
        else {
            buf.skipBytes(dataLength);
            if (type != MSG_COMMAND_0 && type != MSG_COMMAND_1 && type != MSG_COMMAND_2) {
                sendResponse(channel, type, buf.readUnsignedShort());
            }
        }

        return null;
    }

}
