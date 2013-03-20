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

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class GalileoProtocolDecoder extends BaseProtocolDecoder {

    public GalileoProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }
    
    private static final Map<Integer, Integer> tagLengthMap = new HashMap<Integer, Integer>();
    
    static {
        int[] l1 = {0x01,0x02,0x35,0x43,0xc4,0xc5,0xc6,0xc7,0xc8,0xc9,0xca,0xcb,0xcc,0xcd,0xce,0xcf,0xd0,0xd1,0xd2,0xd5};
        int[] l2 = {0x04,0x10,0x34,0x40,0x41,0x42,0x45,0x46,0x50,0x51,0x52,0x53,0x58,0x59,0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,0xd6,0xd7,0xd8,0xd9,0xda};
        int[] l4 = {0x20,0x33,0x44,0x90,0xc0,0xc1,0xc2,0xc3,0xd3,0xd4,0xdb,0xdc,0xdd,0xde,0xdf};
        for (int i : l1) { tagLengthMap.put(i, 1); }
        for (int i : l2) { tagLengthMap.put(i, 2); }
        for (int i : l4) { tagLengthMap.put(i, 4); }
    }

    private static int getTagLength(int tag) {
        return tagLengthMap.get(tag);
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

    private static final int TAG_IMEI = 0x03;
    private static final int TAG_DATE = 0x20;
    private static final int TAG_COORDINATES = 0x30;
    private static final int TAG_SPEED_COURSE = 0x33;
    private static final int TAG_ALTITUDE = 0x34;
    private static final int TAG_STATUS = 0x40;
    private static final int TAG_POWER = 0x41;
    private static final int TAG_BATTERY = 0x42;
    private static final int TAG_MILAGE = 0xd4;

    
    private void sendReply(Channel channel, int checksum) {
        ChannelBuffer reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 3);
        reply.writeByte(0x02);
        reply.writeShort((short) checksum);
        if (channel != null) {
            channel.write(reply);
        }
    }
    
    private Long deviceId;
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        
        buf.readUnsignedByte(); // header
        int length = (buf.readUnsignedShort() & 0x7fff) + 3;
        
        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>galileo</protocol>");
        
        while (buf.readerIndex() < length) {
            int tag = buf.readUnsignedByte();
            switch (tag) {

                case TAG_IMEI:
                    String imei = buf.toString(buf.readerIndex(), 15, Charset.defaultCharset());
                    buf.skipBytes(imei.length());
                    try {
                        deviceId = getDataManager().getDeviceByImei(imei).getId();
                    } catch(Exception error) {
                        Log.warning("Unknown device - " + imei);
                    }
                    break;

                case TAG_DATE:
                    position.setTime(new Date(buf.readUnsignedInt() * 1000));
                    break;
                    
                case TAG_COORDINATES:
                    position.setValid((buf.readUnsignedByte() & 0xf0) == 0x00);
                    position.setLatitude(buf.readInt() / 1000000.0);
                    position.setLongitude(buf.readInt() / 1000000.0);
                    break;
                    
                case TAG_SPEED_COURSE:
                    position.setSpeed(buf.readUnsignedShort() * 0.0539957);
                    position.setCourse(buf.readUnsignedShort() * 0.1);
                    break;
                    
                case TAG_ALTITUDE:
                    position.setAltitude((double) buf.readShort());
                    break;
                    
                case TAG_STATUS:
                    extendedInfo.append("<status>");
                    extendedInfo.append(buf.readUnsignedShort());
                    extendedInfo.append("</status>");
                    break;
                    
                case TAG_POWER:
                    position.setPower((double) buf.readUnsignedShort());
                    break;
                    
                case TAG_BATTERY:
                    extendedInfo.append("<battery>");
                    extendedInfo.append(buf.readUnsignedShort());
                    extendedInfo.append("</battery>");
                    break;
                    
                case TAG_MILAGE:
                    extendedInfo.append("<milage>");
                    extendedInfo.append(buf.readUnsignedInt());
                    extendedInfo.append("</milage>");
                    break;
                    
                default:
                    buf.skipBytes(getTagLength(tag));
                    break;
                    
            }
        }

        if (deviceId == null) {
            Log.warning("Unknown device");
            return null;
        }
        
        position.setDeviceId(deviceId);
        sendReply(channel, buf.readUnsignedShort());

        if (position.getValid() == null || position.getTime() == null || position.getSpeed() == null) {
            return null;
        }

        if (position.getAltitude() == null) {
            position.setAltitude(0.0);
        }
        
        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
