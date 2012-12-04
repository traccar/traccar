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

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Navis protocol decoder
 */
public class NavisProtocolDecoder extends GenericProtocolDecoder {

    private String prefix;
    private long deviceId, serverId;
    
    private static final Charset charset = Charset.defaultCharset();

    private String imei;
    private Long databaseDeviceId;

    /**
     * Initialize
     */
    public NavisProtocolDecoder(DataManager dataManager) {
        super(dataManager);
    }

    // Format types
    public static final int F10 = 0x01;
    public static final int F20 = 0x02;
    public static final int F30 = 0x03;
    public static final int F40 = 0x04;
    public static final int F50 = 0x05;
    public static final int F51 = 0x15;
    public static final int F52 = 0x25;
    
    private static boolean isFormat(int type, int... types) {
        for (int i : types) {
            if (type == i) {
                return true;
            }
        }
        return false;
    }

    private Position parsePosition(ChannelBuffer buf) {
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>navis</protocol>");
        
        position.setDeviceId(databaseDeviceId);
        position.setAltitude(0.0);

        // Format type
        int format;
        if (buf.getUnsignedByte(buf.readerIndex()) == 0) {
            format = buf.readUnsignedShort();
        } else {
            format = buf.readUnsignedByte();
        }
        extendedInfo.append("<format>");
        extendedInfo.append(format);
        extendedInfo.append("</format>");
        
        position.setId(buf.readUnsignedInt()); // sequence number

        // Event type
        extendedInfo.append("<event>");
        extendedInfo.append(buf.readUnsignedShort());
        extendedInfo.append("</event>");
        
        // Event time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR, buf.readUnsignedByte());
        time.set(Calendar.MINUTE, buf.readUnsignedByte());
        time.set(Calendar.SECOND, buf.readUnsignedByte());
        time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
        time.set(Calendar.MONTH, buf.readUnsignedByte());
        time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
        extendedInfo.append("<time>");
        extendedInfo.append(time.getTimeInMillis());
        extendedInfo.append("</time>");
        
        // Alarm status
        extendedInfo.append("<alarm>");
        extendedInfo.append(buf.readUnsignedByte());
        extendedInfo.append("</alarm>");
        
        // Modules status
        extendedInfo.append("<status>");
        extendedInfo.append(buf.readUnsignedByte());
        extendedInfo.append("</status>");
        
        // GSM signal
        extendedInfo.append("<gsm>");
        extendedInfo.append(buf.readUnsignedByte());
        extendedInfo.append("</gsm>");
        
        // Output
        extendedInfo.append("<output>");
        if (isFormat(format, F10, F20, F30)) {
            extendedInfo.append(buf.readUnsignedShort());
        } else if (isFormat(format, F40, F50, F51, F52)) {
            extendedInfo.append(buf.readUnsignedByte());
        }
        extendedInfo.append("</output>");
        
        // Input
        extendedInfo.append("<input>");
        if (isFormat(format, F10, F20, F30, F40)) {
            extendedInfo.append(buf.readUnsignedShort());
        } else if (isFormat(format, F50, F51, F52)) {
            extendedInfo.append(buf.readUnsignedByte());
        }
        extendedInfo.append("</input>");
        
        position.setPower(buf.readUnsignedShort() / 1000.0); // power

        // Battery power
        extendedInfo.append("<battery>");
        extendedInfo.append(buf.readUnsignedShort());
        extendedInfo.append("</battery>");

        // Temperature
        if (isFormat(format, F10, F20, F30)) {
            extendedInfo.append("<temperature>");
            extendedInfo.append(buf.readShort());
            extendedInfo.append("</temperature>");
        }

        if (isFormat(format, F10, F20, F50, F52)) {
            // ADC 1
            extendedInfo.append("<adc1>");
            extendedInfo.append(buf.readUnsignedShort());
            extendedInfo.append("</adc1>");

            // ADC 2
            extendedInfo.append("<adc2>");
            extendedInfo.append(buf.readUnsignedShort());
            extendedInfo.append("</adc2>");
        }
        
        if (isFormat(format, F20, F50, F51, F52)) {
            // Impulse counters
            buf.readUnsignedInt();
            buf.readUnsignedInt();
        }
        
        if (isFormat(format, F20, F50, F51, F52)) {
            // Validity
            int locationStatus = buf.readUnsignedByte();
            position.setValid((locationStatus & 0x02) == 0x02);

            // Location time
            time.clear();
            time.set(Calendar.HOUR, buf.readUnsignedByte());
            time.set(Calendar.MINUTE, buf.readUnsignedByte());
            time.set(Calendar.SECOND, buf.readUnsignedByte());
            time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
            time.set(Calendar.MONTH, buf.readUnsignedByte());
            time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
            position.setTime(time.getTime());
            
            // Location data
            position.setLatitude(buf.readFloat() / Math.PI * 180);
            position.setLongitude(buf.readFloat() / Math.PI * 180);
            position.setSpeed((double) buf.readFloat());
            position.setCourse((double) buf.readUnsignedShort());
            
            // Milage
            extendedInfo.append("<milage>");
            extendedInfo.append(buf.readFloat());
            extendedInfo.append("</milage>");
            
            // Last segment
            extendedInfo.append("<segment>");
            extendedInfo.append(buf.readFloat());
            extendedInfo.append("</segment>");
            
            // Segment times
            buf.readUnsignedShort();
            buf.readUnsignedShort();
        }
        
        if (isFormat(format, F51, F52)) {
            // Other stuff
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
            buf.readUnsignedShort();
            buf.readByte();
            buf.readUnsignedShort();
        }
        
        if (isFormat(format, F40, F52)) {
            // Four temperature sensors
            buf.readByte();
            buf.readByte();
            buf.readByte();
            buf.readByte();
        }

        // Extended info
        position.setExtendedInfo(extendedInfo.toString());

        return position;
    }
    
    private Object processSingle(Channel channel, ChannelBuffer buf) {
        Position position = parsePosition(buf);
        
        ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 8);
        response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<T", charset));
        response.writeInt(position.getId().intValue());
        sendReply(channel, response);

        // No location data
        if (position.getValid() == null) {
            return null;
        }
        
        return position;
    }
    
    private Object processArray(Channel channel, ChannelBuffer buf) {
        List<Position> positions = new LinkedList<Position>();
        int count = buf.readUnsignedByte();

        for (int i = 0; i < count; i++) {
            Position position = parsePosition(buf);
            if (position.getValid() != null) {
                positions.add(position);
            }
        }
        
        ChannelBuffer response = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 8);
        response.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<A", charset));
        response.writeByte(count);
        sendReply(channel, response);

        // No location data
        if (positions.isEmpty()) {
            return null;
        }
        
        return positions;
    }
    
    private Object processHandshake(Channel channel, ChannelBuffer buf) {
        buf.readByte(); // semicolon symbol
        imei = buf.toString(Charset.defaultCharset());

        try {
            databaseDeviceId = getDataManager().getDeviceByImei(imei).getId();
            sendReply(channel, ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, "*<S", charset));
        } catch(Exception error) {
            Log.warning(error.toString());
        }
        return null;
    }
    
    private static short checksum(ChannelBuffer buf) {
        short sum = 0;
        for (int i = 0; i < buf.readableBytes(); i++) {
            sum ^= buf.getUnsignedByte(i);
        }
        return sum;
    }
    
    private void sendReply(Channel channel, ChannelBuffer data) {
        ChannelBuffer header = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 16);
        header.writeBytes(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, prefix, charset));
        header.writeInt((int) deviceId);
        header.writeInt((int) serverId);
        header.writeShort(data.readableBytes());
        header.writeByte(checksum(data));
        header.writeByte(checksum(header));
        
        if (channel != null) {
            channel.write(ChannelBuffers.copiedBuffer(header, data));
        }
    }
    
    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        ChannelBuffer buf = (ChannelBuffer) msg;

        // Read header
        prefix = buf.toString(buf.readerIndex(), 4, charset);
        buf.skipBytes(prefix.length()); // prefix @NTC by default
        serverId = buf.readUnsignedInt();
        deviceId = buf.readUnsignedInt();
        int length = buf.readUnsignedShort();
        buf.skipBytes(2); // header and data XOR checksum
        
        if (length == 0) {
            return null; // keep alive message
        }
        
        // Read message type
        String type = buf.toString(buf.readerIndex(), 3, charset);
        buf.skipBytes(type.length());
        
        if (type.equals("*>T")) {
            return processSingle(channel, buf);
        } else if (type.equals("*>A")) {
            return processArray(channel, buf);
        } else if (type.equals("*>S")) {
            return processHandshake(channel, buf);
        }

        return null;
    }

}
