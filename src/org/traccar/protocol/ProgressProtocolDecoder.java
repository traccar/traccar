/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.TimeZone;
import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Progress tracker protocol decoder
 */
public class ProgressProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Device ID
     */
    private long deviceId;

    /**
     * Initialize
     */
    public ProgressProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    /*
     * Message types
     */
    static final int MSG_NULL = 0;
    static final int MSG_IDENT = 1;
    static final int MSG_IDENT_FULL = 2;
    static final int MSG_POINT = 10;
    static final int MSG_LOG_SYNC = 100;
    static final int MSG_LOGMSG = 101;
    static final int MSG_TEXT = 102;
    static final int MSG_ALARM = 200;
    static final int MSG_ALARM_RECIEVED = 201;

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        int type = buf.readUnsignedShort();
        int length = buf.readUnsignedShort();

        // Authentication
        if (type == MSG_IDENT || type == MSG_IDENT_FULL) {
            long id = buf.readUnsignedInt();
            length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            String imei = buf.readBytes(length).toString(Charset.defaultCharset());
            try {
                deviceId = getDataManager().getDeviceByImei(imei).getId();
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
            }
        }

        // Position
        else if (type == MSG_POINT || type == MSG_ALARM) {
            Position position = new Position();
            StringBuilder extendedInfo = new StringBuilder("<protocol>progress</protocol>");
            position.setDeviceId(deviceId);

            // Message index
            buf.readUnsignedInt();

            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.setTimeInMillis(buf.readUnsignedInt() * 1000);
            position.setTime(time.getTime());

            // Latitude
            position.setLatitude(((double) buf.readInt()) / 0x7FFFFFFF * 180.0);

            // Longitude
            position.setLongitude(((double) buf.readInt()) / 0x7FFFFFFF * 180.0);

            // Speed
            position.setSpeed(((double) buf.readUnsignedInt()) / 100);

            // Course
            position.setCourse(((double) buf.readUnsignedShort()) / 100);

            // Altitude
            position.setAltitude(((double) buf.readUnsignedShort()) / 100);

            // Satellites
            int satellitesNumber = buf.readUnsignedByte();
            extendedInfo.append("<sat>");
            extendedInfo.append(satellitesNumber);
            extendedInfo.append("</sat>");
            position.setValid(satellitesNumber >= 3); // TODO: probably wrong

            // Cell signal
            extendedInfo.append("<gsm>");
            extendedInfo.append(buf.readUnsignedByte());
            extendedInfo.append("</gsm>");

            // TODO: process other data

            // Extended info
            position.setExtendedInfo(extendedInfo.toString());

            // Send response for alarm message
            if (type == MSG_ALARM) {
                byte[] response = {(byte)0xC9,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
                channel.write(ChannelBuffers.wrappedBuffer(response));

                extendedInfo.append("<alarm>true</alarm>");
            }

            return position;
        }

        return null;
    }

}
