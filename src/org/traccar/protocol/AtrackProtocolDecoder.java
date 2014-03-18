/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class AtrackProtocolDecoder extends BaseProtocolDecoder {

    public AtrackProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private static final int MSG_HEARTBEAT = 0x1A;
    private static final int MSG_DATA = 0x10;

    private static void sendResponse(Channel channel, long rawId, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(12);
            response.writeShort(0xfe02);
            response.writeLong(rawId);
            response.writeShort(index);
            channel.write(response);
        }
    }
    
    private static String readString(ChannelBuffer buf) {
        
        int length = 0;
        while (buf.getByte(buf.readerIndex() + length) != 0) {
            length += 1;
        }
        if (length != 0) {
            String result = buf.toString(buf.readerIndex(), length, Charset.defaultCharset());
            buf.skipBytes(length);
            return result;
        }
        buf.readByte();
        
        return null;
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        buf.skipBytes(2); // prefix
        buf.readUnsignedShort(); // checksum
        buf.readUnsignedShort(); // length
        int index = buf.readUnsignedShort();

        // Get device id
        long deviceId;
        long rawId = buf.readLong();
        String id = String.valueOf(rawId);
        try {
            deviceId = getDataManager().getDeviceByImei(id).getId();
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }
        
        // Send acknowledgement
        sendResponse(channel, rawId, index);

        List<Position> positions = new LinkedList<Position>();

        while (buf.readable()) {

            // Create new position
            Position position = new Position();
            position.setDeviceId(deviceId);
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("atrack");

            // Date and time
            position.setTime(new Date(buf.readUnsignedInt() * 1000)); // gps time
            buf.readUnsignedInt(); // rtc time
            buf.readUnsignedInt(); // send time

            // Coordinates
            position.setValid(true);
            position.setLongitude(buf.readInt() * 0.000001);
            position.setLatitude(buf.readInt() * 0.000001);
            position.setAltitude(0.0);

            // Course
            position.setCourse((double) buf.readUnsignedShort());

            // Report type
            extendedInfo.set("type", buf.readUnsignedByte());

            // Milage
            extendedInfo.set("milage", buf.readUnsignedInt() * 0.1);

            // Accuracy
            extendedInfo.set("hdop", buf.readUnsignedShort() * 0.1);

            // Input
            extendedInfo.set("input", buf.readUnsignedByte());

            // Speed
            position.setSpeed(buf.readUnsignedShort() * 0.539957);

            // Output
            extendedInfo.set("output", buf.readUnsignedByte());

            // ADC
            extendedInfo.set("adc", buf.readUnsignedShort() * 0.001);

            // Driver
            extendedInfo.set("driver", readString(buf));

            // Temperature
            extendedInfo.set("temperature1", buf.readShort() * 0.1);
            extendedInfo.set("temperature2", buf.readShort() * 0.1);

            // Text Message
            extendedInfo.set("message", readString(buf));

            // With AT$FORM Command you can extend atrack protocol.
            // For example adding AT$FORM %FC /Fuel used you can add the line in this position:
            // extendedInfo.set("fuelused", buf.readUnsignedInt() * 0.1);

            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }

        return positions;
    }

}
