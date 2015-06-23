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
import java.util.*;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TytanProtocolDecoder extends BaseProtocolDecoder {

    public TytanProtocolDecoder(TytanProtocol protocol) {
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
        if (!identify(id, channel, remoteAddress)) {
            return null;
        }

        List<Position> positions = new LinkedList<Position>();
        
        while (buf.readable()) {
            
            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());
            
            int end = buf.readerIndex() + buf.readUnsignedByte();
            
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            
            int flags = buf.readUnsignedByte();
            position.set(Event.KEY_GPS, flags >> 5);
            position.set(Event.KEY_GSM, flags & 0x07);
            position.setValid(((flags & 0x08) != 0) ^ ((flags & 0x10) != 0));
            
            // Latitude
            double lat = buf.readUnsignedMedium();
            lat = lat * -180 / 16777216 + 90;
            position.setLatitude(lat);
            
            // Longitude
            double lon = buf.readUnsignedMedium();
            lon = lon * 360 / 16777216 - 180;
            position.setLongitude(lon);
            
            // Status
            flags = buf.readUnsignedByte();
            position.set(Event.KEY_STATUS, flags & 0x1f);
            int course = (flags >> 5) * 45;
            course = (course + 180) % 360;
            position.setCourse(course);
            
            // Speed
            int speed = buf.readUnsignedByte();
            if (speed < 250) {
                position.setSpeed(UnitsConverter.knotsFromKph(speed));
            }
            
            while (buf.readerIndex() < end) {
                int x = buf.getUnsignedByte(buf.readerIndex());
                switch (buf.readUnsignedByte()) {
                    case 2:
                        position.set(Event.KEY_ODOMETER, buf.readUnsignedMedium());
                        break;
                    case 4:
                        buf.readUnsignedShort(); // device start
                        break;
                    case 5:
                        position.set(Event.KEY_INPUT, buf.readUnsignedByte());
                        break;
                    case 6:
                        {
                            int n = buf.readUnsignedByte() >> 4;
                            if (n < 2) {
                                position.set(Event.PREFIX_ADC + n, buf.readFloat());
                            } else {
                                position.set("di" + (n - 2), buf.readFloat());
                            }
                        }
                        break;
                    case 7:
                        position.set(Event.KEY_ALARM, buf.readUnsignedShort());
                        break;
                    case 8:
                        position.set("antihijack", buf.readUnsignedByte());
                        break;
                    case 9:
                        position.set("authorized", ChannelBufferTools.readHexString(buf, 16));
                        break;
                    case 10:
                        position.set("unauthorized", ChannelBufferTools.readHexString(buf, 16));
                        break;
                    case 23:
                        buf.skipBytes(9);
                        break;
                    case 24:
                        {
                            Set<Integer> temps = new LinkedHashSet<Integer>();
                            int temp = buf.readUnsignedByte();
                            for (int i = 3; i >= 0; i--) {
                                int n = (temp >> (2 * i)) & 0x03;
                                if (!temps.contains(n)) {
                                    temps.add(n);
                                }
                            }
                            for (int n : temps) {
                                position.set(Event.PREFIX_TEMP + n, buf.readUnsignedByte());
                            }
                        }
                        break;
                    case 25:
                        buf.readUnsignedByte();
                        buf.readUnsignedShort(); // fuel
                        break;
                    case 26:
                        buf.skipBytes(buf.readUnsignedByte() * 2); // flowmeter
                        break;
                    case 28:
                        position.set("weight", buf.readUnsignedShort());
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
                    case 90:
                        position.set(Event.KEY_POWER, buf.readFloat());
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
                        position.set(Event.KEY_FUEL, buf.readUnsignedShort() & 0x3fff);
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
                        position.set("door", buf.readUnsignedByte());
                        break;
                }
            }
            positions.add(position);
        }
        
        return positions;
    }

}
