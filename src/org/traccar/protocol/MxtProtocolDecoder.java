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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Date;

public class MxtProtocolDecoder extends BaseProtocolDecoder {

    public MxtProtocolDecoder(String protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        /*buf.readByte(); // header
        buf.readUnsignedByte(); // version
        buf.readUnsignedByte(); // type

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocol());

        // Get device id
        String imei = ChannelBufferTools.readHexString(buf, 16).substring(1);
        if (!identify(imei)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        long seconds = buf.readUnsignedInt() & 0x7fffffffl;
        seconds += 946684800l; // 2000-01-01 00:00
        position.setTime(new Date(seconds * 1000));
        
        boolean hasLocation = false;

        while (buf.readableBytes() > 3) {

            short type = buf.readUnsignedByte();
            short length = buf.readUnsignedByte();

            switch (type) {

                case DATA_GPS:
                    hasLocation = true;
                    position.setValid(true);
                    position.setLatitude(buf.readInt() / 1000000.0);
                    position.setLongitude(buf.readInt() / 1000000.0);
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    position.setCourse(buf.readUnsignedShort());
                    position.set(Event.KEY_HDOP, buf.readUnsignedShort());
                    break;

                default:
                    buf.skipBytes(length);
                    break;
            }
        }

        if (hasLocation) {
            return position;
        }*/
        return null;
    }

}
