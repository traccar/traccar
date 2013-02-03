/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Position;

public class TeltonikaProtocolDecoder extends BaseProtocolDecoder {
    
    private long deviceId;

    public TeltonikaProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private void parseIdentification(Channel channel, ChannelBuffer buf) {
        boolean result = false;

        int length = buf.readUnsignedShort();
        String imei = buf.toString(buf.readerIndex(), length, Charset.defaultCharset());
        try {
            deviceId = getDataManager().getDeviceByImei(imei).getId();
            result = true;
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
        }
        
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(1);
            response.writeByte(result ? 1 : 0);
            channel.write(response);
        }
    }
    
    private List<Position> parseLocation(Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<Position>();
        
        buf.skipBytes(4); // marker
        buf.readUnsignedInt(); // data length
        buf.readUnsignedByte(); // codec
        int count = buf.readUnsignedByte();
        
        for (int i = 0; i < count; i++) {
            Position position = new Position();
            StringBuilder extendedInfo = new StringBuilder("<protocol>teltonika</protocol>");
            
            position.setTime(new Date(buf.readLong()));
            
            extendedInfo.append("<priority>");
            extendedInfo.append(buf.readUnsignedByte());
            extendedInfo.append("</priority>");

            position.setLongitude(buf.readUnsignedInt() / 10000000.0);
            position.setLatitude(buf.readUnsignedInt() / 10000000.0);
            position.setAltitude((double) buf.readUnsignedShort());
            position.setCourse((double) buf.readUnsignedShort());
            
            extendedInfo.append("<satellites>");
            int satellites = buf.readUnsignedByte();
            extendedInfo.append(satellites);
            extendedInfo.append("</satellites>");

            position.setValid(satellites != 0);
            
            position.setSpeed((double) buf.readUnsignedShort());
            
            extendedInfo.append("<event>");
            extendedInfo.append(buf.readUnsignedByte());
            extendedInfo.append("</event>");
            
            // Skip IO data
            buf.readUnsignedByte(); // total IO data records
            buf.skipBytes(buf.readUnsignedByte() * (1 + 1));
            buf.skipBytes(buf.readUnsignedByte() * (1 + 2));
            buf.skipBytes(buf.readUnsignedByte() * (1 + 4));
            buf.skipBytes(buf.readUnsignedByte() * (1 + 8));
        
            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }
        
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(4);
            response.writeInt(count);
            channel.write(response);
        }
        
        return positions;
    }
    
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;
        
        if (buf.getUnsignedShort(0) > 0) {
            parseIdentification(channel, buf);
        } else {
            return parseLocation(channel, buf);
        }
        
        return null;
    }

}
