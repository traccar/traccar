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
import java.util.Date;
import java.util.TimeZone;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MxtProtocolDecoder extends BaseProtocolDecoder {

    public MxtProtocolDecoder(MxtProtocol protocol) {
        super(protocol);
    }

    private static final int MSG_ACK = 0x02;
    private static final int MSG_NACK = 0x03;
    private static final int MSG_POSITION = 0x31;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // start
        buf.readUnsignedByte(); // device descriptor
        int type = buf.readUnsignedByte();

        String id = String.valueOf(buf.readUnsignedInt());
        if (!identify(id, channel)) {
            return null;
        }

        if (type == MSG_POSITION) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
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

            long days = BitUtil.range(date, 6 + 6 + 5);
            long hours = BitUtil.range(date, 6 + 6, 5);
            long minutes = BitUtil.range(date, 6, 6);
            long seconds = BitUtil.range(date, 0, 6);

            long millis = time.getTimeInMillis();
            millis += (((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1000;

            position.setTime(new Date(millis));

            // Location
            position.setValid(true);
            position.setLatitude(buf.readInt() / 1000000.0);
            position.setLongitude(buf.readInt() / 1000000.0);

            long flags = buf.readUnsignedInt();
            position.set(Event.KEY_IGNITION, BitUtil.check(flags, 0));
            position.set(Event.KEY_ALARM, BitUtil.check(flags, 1));
            position.set(Event.KEY_INPUT, BitUtil.range(flags, 2, 5));
            position.set(Event.KEY_OUTPUT, BitUtil.range(flags, 7, 3));
            position.setCourse(BitUtil.range(flags, 10, 3) * 45);
            //position.setValid(BitUtil.check(flags, 15));
            position.set(Event.KEY_CHARGE, BitUtil.check(flags, 20));

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

            int inputMask = buf.readUnsignedByte();
            
            if (BitUtil.check(infoGroups, 0)) {
                buf.skipBytes(8); // waypoints
            }
            
            if (BitUtil.check(infoGroups, 1)) {
                buf.skipBytes(8); // wireless accessory
            }
            
            if (BitUtil.check(infoGroups, 2)) {
                position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
                position.set(Event.KEY_HDOP, buf.readUnsignedByte());
                buf.readUnsignedByte(); // GPS accuracy
                position.set(Event.KEY_GSM, buf.readUnsignedByte());
                buf.readUnsignedShort(); // time since boot
                buf.readUnsignedByte(); // input voltage
                position.set(Event.PREFIX_TEMP + 1, buf.readByte());
            }
            
            if (BitUtil.check(infoGroups, 3)) {
                position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
            }
            
            if (BitUtil.check(infoGroups, 4)) {
                position.set("hours", buf.readUnsignedInt());
            }
            
            if (BitUtil.check(infoGroups, 5)) {
                buf.readUnsignedInt(); // reason
            }
            
            if (BitUtil.check(infoGroups, 6)) {
                position.set(Event.KEY_POWER, buf.readUnsignedShort() * 0.001);
                position.set(Event.KEY_BATTERY, buf.readUnsignedShort());
            }
            
            if (BitUtil.check(infoGroups, 7)) {
                position.set(Event.KEY_RFID, buf.readUnsignedInt());
            }

            return position;
        }

        return null;
    }

}
