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

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class TytanProtocolDecoder extends BaseProtocolDecoder {

    public TytanProtocolDecoder(String protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        
        buf.readUnsignedByte(); // protocol
        int index = buf.readUnsignedByte() >> 3;
        
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.copiedBuffer(
                    "^" + index, Charset.defaultCharset());
            channel.write(response, remoteAddress);
        }
        
        String id = String.valueOf(buf.readUnsignedInt());
        if (!identify(id)) {
            return null;
        }

        List<Position> positions = new LinkedList<Position>();
        
        while (buf.readable()) {
            
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            position.setDeviceId(getDeviceId());
            
            int end = buf.readerIndex() + buf.readUnsignedByte();
            
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            
            int flags = buf.readUnsignedByte();
            extendedInfo.set("gps", flags >> 5);
            extendedInfo.set("gsm", flags & 0x07);
            position.setValid(((flags & 0x08) != 0) ^ ((flags & 0x10) != 0));
            
            // Latitude
            double lat = buf.readUnsignedMedium();
            lat = lat * -180 / 16777216 + 90;
            position.setLatitude(lat);
            
            // Longitude
            double lon = buf.readUnsignedMedium();
            lon = lon * 360 / 16777216 - 180;
            position.setLongitude(lon);
            
            // Altitude
            position.setAltitude(0.0);
            
            // Course
            int course = (buf.readUnsignedByte() >> 5) * 45;
            course = (course + 180) % 360;
            position.setCourse((double) course);
            
            // Speed
            int speed = buf.readUnsignedByte();
            if (speed >= 250) {
                position.setSpeed(0.0);
            } else {
                position.setSpeed(speed * 0.539957);
            }
            
            buf.skipBytes(end - buf.readerIndex());
            
            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }
        
        return positions;
    }

}
