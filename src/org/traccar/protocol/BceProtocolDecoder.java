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

import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class BceProtocolDecoder extends BaseProtocolDecoder {

    public BceProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final int MSG_HEARTBEAT = 0x1A;
    private static final int MSG_DATA = 0x10;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        /*buf.skipBytes(2); // header
        buf.readByte(); // size

        // Zero for location messages
        buf.readByte(); // voltage
        buf.readByte(); // gsm signal

        String imei = readImei(buf);
        long index = buf.readUnsignedShort();
        int type = buf.readUnsignedByte();

        if (type == MSG_HEARTBEAT) {
            if (channel != null) {
                byte[] response = {0x54, 0x68, 0x1A, 0x0D, 0x0A};
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }
        }

        else if (type == MSG_DATA) {

            // Create new position
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            extendedInfo.set("index", index);

            // Get device id
            try {
                position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
                return null;
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
            position.setSpeed((double) buf.readUnsignedByte());

            // Course
            position.setCourse((double) buf.readUnsignedShort());

            buf.skipBytes(3); // reserved

            // Flags
            long flags = buf.readUnsignedInt();
            position.setValid((flags & 0x1) == 0x1);
            if ((flags & 0x2) == 0) latitude = -latitude;
            if ((flags & 0x4) == 0) longitude = -longitude;

            position.setLatitude(latitude);
            position.setLongitude(longitude);
            position.setAltitude(0.0);

            position.setExtendedInfo(extendedInfo.toString());
            return position;
        }*/

        return null;
    }

}
