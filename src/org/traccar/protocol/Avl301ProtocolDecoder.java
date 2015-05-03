/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Calendar;
import java.util.TimeZone;

public class Avl301ProtocolDecoder extends BaseProtocolDecoder {

    public Avl301ProtocolDecoder(String protocol) {
        super(protocol);
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

    private static final int MSG_LOGIN = 'L';
    private static final int MSG_STATUS = 'H';
    private static final int MSG_GPS_LBS_STATUS = '$';

    private static void sendResponse(Channel channel, int type) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(5);
            response.writeByte('$');
            response.writeByte(type);
            response.writeByte('#');
            response.writeByte('\r'); response.writeByte('\n');
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(1); // header
        int type = buf.readUnsignedByte();
        buf.readUnsignedByte(); // length

        if (type == MSG_LOGIN) {

            if (identify(readImei(buf))) {
                sendResponse(channel, type);
            }

        } else if (hasDeviceId() && type == MSG_STATUS) {

            sendResponse(channel, type);

        } else if (hasDeviceId() && type == MSG_GPS_LBS_STATUS) {

            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());

            // Date and time(6)
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
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

            //Skip Satellite numbers
            buf.skipBytes(1);

            // Location
            double latitude = buf.readUnsignedInt() / 600000.0;
            double longitude = buf.readUnsignedInt() / 600000.0;
            position.setSpeed(buf.readUnsignedByte() * 1.0); // kph?

            // Course and flags
            int union = buf.readUnsignedShort();
            position.setCourse(union & 0x03FF);
            position.setValid((union & 0x1000) != 0);
            if ((union & 0x0400) != 0) latitude = -latitude;
            if ((union & 0x0800) != 0) longitude = -longitude;

            position.setLatitude(latitude);
            position.setLongitude(longitude);

            if ((union & 0x4000) != 0) {
                position.set("acc", (union & 0x8000) != 0);
            }

            position.set(Event.KEY_LAC, buf.readUnsignedShort());
            position.set(Event.KEY_CELL, buf.readUnsignedMedium());
            position.set(Event.KEY_ALARM, true);
            int flags = buf.readUnsignedByte();
            position.set("acc", (flags & 0x2) != 0);

            // TODO parse other flags

            position.set(Event.KEY_POWER, buf.readUnsignedByte());
            position.set(Event.KEY_GSM, buf.readUnsignedByte());
            return position;
        }

        return null;
    }

}
