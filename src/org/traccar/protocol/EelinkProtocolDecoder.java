/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Date;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class EelinkProtocolDecoder extends BaseProtocolDecoder {

    public EelinkProtocolDecoder(EelinkProtocol protocol) {
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

    private static final int MSG_LOGIN = 0x01;
    private static final int MSG_GPS = 0x02;
    private static final int MSG_HEARTBEAT = 0x03;
    private static final int MSG_ALARM = 0x04;
    private static final int MSG_STATE = 0x05;
    private static final int MSG_SMS = 0x06;
    private static final int MSG_OBD = 0x07;
    private static final int MSG_INTERACTIVE = 0x80;
    private static final int MSG_DATA = 0x81;

    private void sendResponse(Channel channel, int type, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.buffer(7);
            response.writeByte(0x67); response.writeByte(0x67); // header
            response.writeByte(type);
            response.writeShort(2); // length
            response.writeShort(index);
            channel.write(response);
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        int type = buf.readUnsignedByte();
        buf.readShort(); // length
        int index = buf.readUnsignedShort();
        
        if (type != MSG_GPS && type != MSG_DATA) {
            sendResponse(channel, type, index);
        }
        
        if (type == MSG_LOGIN) {
            identify(ChannelBufferTools.readHexString(buf, 16).substring(1), channel);
        }
        
        else if (hasDeviceId() &&
                (type == MSG_GPS ||
                 type == MSG_ALARM ||
                 type == MSG_STATE ||
                 type == MSG_SMS)) {
            
            // Create new position
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            
            position.setProtocol(getProtocolName());
            position.set(Event.KEY_INDEX, index);
            
            // Location
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            position.setCourse(buf.readUnsignedShort());
            
            // Cell
            position.set(Event.KEY_CELL, ChannelBufferTools.readHexString(buf, 18));
            
            // Validity
            position.setValid((buf.readUnsignedByte() & 0x01) != 0);
            
            if (type == MSG_ALARM) {
                position.set(Event.KEY_ALARM, buf.readUnsignedByte());
            }
            
            if (type == MSG_STATE) {
                position.set(Event.KEY_STATUS, buf.readUnsignedByte());
            }
            return position;
        }

        /*
        if (type == MSG_HEARTBEAT) {
            if (channel != null) {
                byte[] response = {0x54, 0x68, 0x1A, 0x0D, 0x0A};
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }
        }

        else if (type == MSG_DATA) {

            // Create new position
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("gt02");
            position.set(Event.KEY_INDEX, index);

            // Get device id
            try {
                position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
            }

            // Date and time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
            time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
            time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
            time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
            time.set(Calendar.MINUTE, buf.readUnsignedByte());
            time.set(Calendar.SECOND, buf.readUnsignedByte());
            position.setTime(time.getTime());

            // Latitude
            double latitude = buf.readUnsignedInt() / (60.0 * 30000.0);

            // Longitude
            double longitude = buf.readUnsignedInt() / (60.0 * 30000.0);

            // Speed
            position.setSpeed(buf.readUnsignedByte());

            // Course
            position.setCourse(buf.readUnsignedShort());

            buf.skipBytes(3); // reserved

            // Flags
            long flags = buf.readUnsignedInt();
            position.setValid((flags & 0x1) == 0x1);
            if ((flags & 0x2) == 0) latitude = -latitude;
            if ((flags & 0x4) == 0) longitude = -longitude;

            position.setLatitude(latitude);
            position.setLongitude(longitude);
            return position;
        }*/

        return null;
    }

}
