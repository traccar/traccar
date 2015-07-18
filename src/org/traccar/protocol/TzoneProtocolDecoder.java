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
import java.util.Calendar;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TzoneProtocolDecoder extends BaseProtocolDecoder {

    public TzoneProtocolDecoder(TzoneProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // type
        buf.readUnsignedShort(); // model
        buf.readUnsignedInt(); // firmware
        
        String imei = ChannelBufferTools.readHexString(buf, 16).substring(1);
        if (!identify(imei, channel)) {
            return null;
        }
        
        buf.skipBytes(6); // device time

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;
        
        if (blockLength == 0) {
            return null;
        }
        
        position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
        
        double lat = buf.readUnsignedInt() / 600000.0;
        double lon = buf.readUnsignedInt() / 600000.0;
        
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
        time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
        time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
        time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
        time.set(Calendar.MINUTE, buf.readUnsignedByte());
        time.set(Calendar.SECOND, buf.readUnsignedByte());
        position.setTime(time.getTime());
        
        position.setSpeed(buf.readUnsignedShort() * 0.01);
        
        position.set(Event.KEY_ODOMETER, buf.readUnsignedMedium());
        
        int flags = buf.readUnsignedShort();
        position.setCourse(BitUtil.range(flags, 0, 9));
        position.setLatitude(BitUtil.check(flags, 10) ? lat : -lat);
        position.setLongitude(BitUtil.check(flags, 9) ? -lon : lon);
        position.setValid(BitUtil.check(flags, 11));
        
        buf.readerIndex(blockEnd);

        return position;
    }

}
