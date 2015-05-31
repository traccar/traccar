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
import java.util.Calendar;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.Crc;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class CastelProtocolDecoder extends BaseProtocolDecoder {

    public CastelProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final int MSG_LOGIN = 0x0110;
    private static final int MSG_LOGIN_RESPONSE = 0x0190;
    private static final int MSG_HEARTBEAT = 0x0301;
    private static final int MSG_HEARTBEAT_RESPONSE = 0x0390;
    private static final int MSG_GPS = 0x0140;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        int version = buf.readUnsignedByte();
        ChannelBuffer id = buf.readBytes(20);
        int type = buf.readUnsignedShort();
        
        if (type == MSG_HEARTBEAT) {
            
            if (channel != null) {
                ChannelBuffer response = ChannelBuffers.directBuffer(31);
                response.writeByte(0x40); response.writeByte(0x40); // header
                response.writeShort(response.capacity()); // size
                response.writeByte(version);
                response.writeBytes(id);
                response.writeShort(MSG_HEARTBEAT_RESPONSE);
                response.writeShort(Crc.crc16Ccitt(response.toByteBuffer(2, 4))); // TODO
                response.writeByte(0x0D); response.writeByte(0x0A); // ending
                channel.write(response, remoteAddress);
            }

        } else if (type == MSG_LOGIN || type == MSG_GPS) {
            
            Position position = new Position();
            position.setDeviceId(getDeviceId());
            position.setProtocol(getProtocol());
            
            if (!identify(id.toString(Charset.defaultCharset()))) {
                return null;
            } else if (type == MSG_LOGIN) {

                if (channel != null) {
                    ChannelBuffer response = ChannelBuffers.directBuffer(41);
                    response.writeByte(0x40); response.writeByte(0x40); // header
                    response.writeShort(response.capacity()); // size
                    response.writeByte(version);
                    response.writeBytes(id);
                    response.writeShort(MSG_LOGIN_RESPONSE);
                    response.writeInt(0xFFFFFFFF);
                    response.writeShort(0);
                    response.writeInt(0); // TODO
                    response.writeShort(Crc.crc16Ccitt(response.toByteBuffer(2, 4))); // TODO
                    response.writeByte(0x0D); response.writeByte(0x0A); // ending
                    channel.write(response, remoteAddress);
                }
            
            }
            
            if (type == MSG_GPS) {
                buf.readUnsignedByte(); // historical
            }
            
            buf.readUnsignedInt(); // ACC ON time
            buf.readUnsignedInt(); // UTC time
            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
            buf.readUnsignedInt(); // trip odometer
            buf.readUnsignedInt(); // total fuel consumption
            buf.readUnsignedShort(); // current fuel consumption
            position.set(Event.KEY_STATUS, buf.readUnsignedInt());
            buf.skipBytes(8);
            
            buf.readUnsignedByte(); // count
            
            // Date and time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
            time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
            time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
            time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
            time.set(Calendar.MINUTE, buf.readUnsignedByte());
            time.set(Calendar.SECOND, buf.readUnsignedByte());
            position.setTime(time.getTime());
            
            double lat = buf.readUnsignedInt() / 3600000.0;
            double lon = buf.readUnsignedInt() / 3600000.0;
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            
            int flags = buf.readUnsignedByte();
            position.setLatitude((flags & 0x01) == 0 ? -lat : lat);
            position.setLongitude((flags & 0x02) == 0 ? -lon : lon);
            position.setValid((flags & 0x0C) > 0);
            position.set(Event.KEY_SATELLITES, flags >> 4);
            
            return position;
        }
        
        return null;
    }

}
