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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
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
            
            // Course
            int course = (buf.readUnsignedByte() >> 5) * 45;
            course = (course + 180) % 360;
            position.setCourse(course);
            
            // Speed
            int speed = buf.readUnsignedByte();
            if (speed < 250) {
                position.setSpeed(speed * 0.539957);
            }
            
            while (buf.readerIndex() < end) {
                switch (buf.readUnsignedByte()) {
                    case 2:
                        extendedInfo.set("milage", buf.readUnsignedMedium());
                        break;
                    case 4:
                        buf.readUnsignedShort(); // device start
                        break;
                    case 5:
                        extendedInfo.set("input", buf.readUnsignedByte());
                        break;
                    case 6:
                        buf.readUnsignedShort();
                        extendedInfo.set("adc", buf.readFloat());
                        break;
                    case 7:
                        extendedInfo.set("alarm", buf.readUnsignedShort());
                        break;
                    case 8:
                        extendedInfo.set("antihijack", buf.readUnsignedByte());
                        break;
                    case 9:
                        extendedInfo.set("authorized", ChannelBufferTools.readHexString(buf, 16));
                        break;
                    case 10:
                        extendedInfo.set("unauthorized", ChannelBufferTools.readHexString(buf, 16));
                        break;
                    case 24:
                        buf.readUnsignedByte();
                        extendedInfo.set("temperature", buf.readUnsignedByte());
                        break;
                    case 25:
                        buf.readUnsignedByte();
                        buf.readUnsignedShort(); // fuel
                        break;
                    case 26:
                        buf.skipBytes(buf.readUnsignedByte() * 2); // flowmeter
                        break;
                    case 28:
                        extendedInfo.set("weight", buf.readUnsignedShort());
                        buf.readUnsignedByte();
                        break;
                    case 29:
                        buf.readUnsignedByte(); // diagnostics
                        break;
                    case 30:
                        buf.readUnsignedByte(); // vending machine
                        buf.readUnsignedInt();
                        buf.readUnsignedInt();
                        buf.readUnsignedInt();
                        break;
                    case 31:
                        buf.readUnsignedByte(); // antihijack
                        break;
                    case 32:
                        buf.readUnsignedByte(); // audio
                        break;
                    case 33:
                        buf.readUnsignedByte(); // antihijack and authorization
                        break;
                    case 80:
                    case 81:
                    case 82:
                    case 83:
                        buf.readUnsignedInt(); // diagnostic
                        break;
                    case 99:
                        buf.readUnsignedInt(); // tachograph
                        break;
                    case 101:
                        buf.readUnsignedByte(); // speed
                        break;
                    case 102:
                        buf.readUnsignedByte(); // engine rpm
                        break;
                    case 103:
                        buf.readUnsignedByte(); // engine temperature
                        break;
                    case 104:
                        buf.readUnsignedByte(); // pedal position
                        break;
                    case 105:
                        buf.readUnsignedByte(); // engine load
                        break;
                    case 107:
                        extendedInfo.set("fuel", buf.readUnsignedShort() & 0x3fff);
                        break;
                    case 108:
                        buf.readUnsignedInt(); // total distance
                        break;
                    case 109:
                        buf.readUnsignedByte(); // ambient temperature
                        break;
                    case 122:
                        buf.readUnsignedByte(); // power take-off state
                        break;
                    case 127:
                        buf.readUnsignedInt(); // total fuel used
                        break;
                    case 129:
                        buf.readUnsignedInt(); // engine total hours
                        break;
                    case 130:
                        buf.readUnsignedShort(); // distance to service
                        break;
                    case 131:
                        buf.readUnsignedShort(); // axle weight
                        buf.readUnsignedShort();
                        buf.readUnsignedShort();
                        buf.readUnsignedShort();
                        break;
                    case 136:
                        buf.readUnsignedShort(); // fuel rate
                        break;
                    case 150:
                        extendedInfo.set("door", buf.readUnsignedByte());
                        break;
                }
            }
            
            position.setExtendedInfo(extendedInfo.toString());
            positions.add(position);
        }
        
        return positions;
    }

}
