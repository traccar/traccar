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
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class TeltonikaProtocolDecoder extends BaseProtocolDecoder {
    
    private long deviceId;

    public TeltonikaProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    public TeltonikaProtocolDecoder(ServerManager serverManager, String protocol) {
        super(serverManager, protocol);
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

    private static boolean checkBit(long mask, int bit) {
        long checkMask = 1 << bit;
        return (mask & checkMask) == checkMask;
    }

    private static final int CODEC_GH3000 = 0x07;
    private static final int CODEC_FM4X00 = 0x08;
    private static final int CODEC_12 = 0x0C;
    
    private List<Position> parseLocation(Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<Position>();
        
        buf.skipBytes(4); // marker
        buf.readUnsignedInt(); // data length
        int codec = buf.readUnsignedByte(); // codec
        
        if (codec == CODEC_12) {
            // TODO: decode serial port data
            return null;
        }
        
        int count = buf.readUnsignedByte();
        
        for (int i = 0; i < count; i++) {
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            
            position.setDeviceId(deviceId);
            
            int globalMask = 0x0f;
            
            if (codec == CODEC_GH3000) {

                long time = buf.readUnsignedInt() & 0x3fffffff;
                time += 1167609600; // 2007-01-01 00:00:00
                position.setTime(new Date(time * 1000));
                
                globalMask = buf.readUnsignedByte();
                if (!checkBit(globalMask, 0)) {
                    return null;
                }
                
                int locationMask = buf.readUnsignedByte();
                
                if (checkBit(locationMask, 0)) {
                    position.setLatitude(Double.valueOf(buf.readFloat()));
                    position.setLongitude(Double.valueOf(buf.readFloat()));
                }
                
                if (checkBit(locationMask, 1)) {
                    position.setAltitude((double) buf.readUnsignedShort());
                }
                
                if (checkBit(locationMask, 2)) {
                    position.setCourse(buf.readUnsignedByte() * 360.0 / 256);
                }
                
                if (checkBit(locationMask, 3)) {
                    position.setSpeed(buf.readUnsignedByte() * 0.539957);
                }
                
                if (checkBit(locationMask, 4)) {
                    int satellites = buf.readUnsignedByte();
                    extendedInfo.set("satellites", satellites);
                    position.setValid(satellites >= 3);
                }
                
                if (checkBit(locationMask, 5)) {
                    extendedInfo.set("area", buf.readUnsignedShort());
                    extendedInfo.set("cell", buf.readUnsignedShort());
                }
                
                if (checkBit(locationMask, 6)) {
                    extendedInfo.set("gsm", buf.readUnsignedByte());
                }
                
                if (checkBit(locationMask, 7)) {
                    extendedInfo.set("operator", buf.readUnsignedInt());
                }

            } else {

                position.setTime(new Date(buf.readLong()));

                extendedInfo.set("priority", buf.readUnsignedByte());

                position.setLongitude(buf.readInt() / 10000000.0);
                position.setLatitude(buf.readInt() / 10000000.0);
                position.setAltitude((double) buf.readShort());
                position.setCourse((double) buf.readUnsignedShort());

                int satellites = buf.readUnsignedByte();
                extendedInfo.set("satellites", satellites);

                position.setValid(satellites != 0);

                position.setSpeed(buf.readUnsignedShort() * 0.539957);

                extendedInfo.set("event", buf.readUnsignedByte());

                buf.readUnsignedByte(); // total IO data records

            }
            
            // Read 1 byte data
            if (checkBit(globalMask, 1)) {
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    extendedInfo.set("io" + buf.readUnsignedByte(), buf.readUnsignedByte());
                }
            }

            
            // Read 2 byte data
            if (checkBit(globalMask, 2)) {
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    extendedInfo.set("io" + buf.readUnsignedByte(), buf.readUnsignedShort());
                }
            }

            // Read 4 byte data
            if (checkBit(globalMask, 3)) {
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    extendedInfo.set("io" + buf.readUnsignedByte(), buf.readUnsignedInt());
                }
            }

            // Read 8 byte data
            if (codec == CODEC_FM4X00) {
                int cnt = buf.readUnsignedByte();
                for (int j = 0; j < cnt; j++) {
                    extendedInfo.set("io" + buf.readUnsignedByte(), buf.readLong());
                }
            }
        
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
