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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MxtProtocolDecoder extends BaseProtocolDecoder {

    public MxtProtocolDecoder(String protocol) {
        super(protocol);
    }

    private static final int MSG_ACK = 0x02;
    private static final int MSG_NACK = 0x03;
    private static final int MSG_POSITION = 0x31;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // start
        buf.readUnsignedByte(); // device descriptor
        int type = buf.readUnsignedByte();

        String id = String.valueOf(buf.readUnsignedInt());
        if (!identify(id)) {
            return null;
        }

        if (type == MSG_POSITION) {

            Position position = new Position();
            position.setProtocol(getProtocol());
            position.setDeviceId(getDeviceId());

            buf.readUnsignedByte(); // protocol
            int infoGroups = buf.readUnsignedByte();

            position.set(Event.KEY_INDEX, buf.readUnsignedShort());

            // Date and time
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000);
            time.set(Calendar.MONTH, 0);
            time.set(Calendar.DAY_OF_MONTH, 1);

            long date = buf.readUnsignedInt();

            long days = date >> (5 + 6 + 6);
            long hours = (date >> (6 + 6)) & 0x1f;
            long minutes = (date >> 6) & 0x3f;
            long seconds = date & 0x3f;

            long millis = time.getTimeInMillis();
            millis += (((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1000;

            position.setTime(new Date(millis));

            // Location
            position.setLatitude(buf.readInt() / 1000000.0);
            position.setLongitude(buf.readInt() / 1000000.0);

            long flags = buf.readUnsignedInt();

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

            int inputMask = buf.readUnsignedByte();

            return position;
        }

        return null;
    }

}
