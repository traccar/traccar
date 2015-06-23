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
import java.util.Calendar;
import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CellocatorProtocolDecoder extends BaseProtocolDecoder {

    public CellocatorProtocolDecoder(CellocatorProtocol protocol) {
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
    
    static final int MSG_CLIENT_STATUS = 0;
    static final int MSG_CLIENT_PROGRAMMING = 3;
    static final int MSG_CLIENT_SERIAL_LOG = 7;
    static final int MSG_CLIENT_SERIAL = 8;
    static final int MSG_CLIENT_MODULAR = 9;

    private static final int MSG_SERVER_ACKNOWLEDGE = 4;
    
    private byte commandCount;
    
    private void sendReply(Channel channel, long deviceId, byte packetNumber) {
        ChannelBuffer reply = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 28);
        reply.writeByte('M');
        reply.writeByte('C');
        reply.writeByte('G');
        reply.writeByte('P');
        reply.writeByte(MSG_SERVER_ACKNOWLEDGE);
        reply.writeInt((int) deviceId);
        reply.writeByte(commandCount++);
        reply.writeInt(0); // authentication code
        reply.writeByte(0);
        reply.writeByte(packetNumber);
        reply.writeZero(11);

        byte checksum = 0;
        for (int i = 4; i < 27; i++) {
            checksum += reply.getByte(i);
        }
        reply.writeByte(checksum);

        if (channel != null) {
            channel.write(reply);
        }
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(4); // system code
        int type = buf.readUnsignedByte();
        long deviceUniqueId = buf.readUnsignedInt();
        
        if (type != MSG_CLIENT_SERIAL) {
            buf.readUnsignedShort(); // communication control
        }
        byte packetNumber = buf.readByte();

        // Send reply
        sendReply(channel, deviceUniqueId, packetNumber);

        // Parse location
        if (type == MSG_CLIENT_STATUS) {
            Position position = new Position();
            position.setProtocol(getProtocolName());
            
            // Device identifier
            if (!identify(String.valueOf(deviceUniqueId), channel)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            buf.readUnsignedByte(); // hardware version
            buf.readUnsignedByte(); // software version
            buf.readUnsignedByte(); // protocol version

            // Status
            position.set(Event.KEY_STATUS, buf.getUnsignedByte(buf.readerIndex()) & 0x0f);
            
            int operator = (buf.readUnsignedByte() & 0xf0) << 4;
            operator += buf.readUnsignedByte();
            
            buf.readUnsignedByte(); // reason data
            buf.readUnsignedByte(); // reason
            buf.readUnsignedByte(); // mode
            buf.readUnsignedInt(); // IO
            
            operator <<= 8;
            operator += buf.readUnsignedByte();
            position.set("operator", operator);
            
            buf.readUnsignedInt(); // ADC
            buf.readUnsignedMedium(); // Odometer
            buf.skipBytes(6); // multi-purpose data
            
            buf.readUnsignedShort(); // gps fix
            buf.readUnsignedByte(); // location status
            buf.readUnsignedByte(); // mode 1
            buf.readUnsignedByte(); // mode 2

            position.setValid(buf.readUnsignedByte() >= 3); // satellites

            // Location data
            position.setLongitude(buf.readInt() / Math.PI * 180 / 100000000);
            position.setLatitude(buf.readInt() / Math.PI * 180 / 100000000.0);
            position.setAltitude(buf.readInt() * 0.01);
            position.setSpeed(UnitsConverter.knotsFromMps(buf.readInt() * 0.01));
            position.setCourse(buf.readUnsignedShort() / Math.PI * 180.0 / 1000.0);
            
            // Time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.SECOND, buf.readUnsignedByte());
            time.set(Calendar.MINUTE, buf.readUnsignedByte());
            time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
            time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
            time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
            time.set(Calendar.YEAR, buf.readUnsignedShort());
            position.setTime(time.getTime());
            return position;
        }

        return null;
    }

}
