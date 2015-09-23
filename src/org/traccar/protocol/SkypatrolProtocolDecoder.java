/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class SkypatrolProtocolDecoder extends BaseProtocolDecoder {
    
    private final long defaultMask;

    public SkypatrolProtocolDecoder(SkypatrolProtocol protocol) {
        super(protocol);
        defaultMask = Context.getConfig().getInteger(getProtocolName() + ".mask");
    }

    private static double convertCoordinate(long coordinate) {
        int sign = 1;
        if (coordinate > 0x7fffffffl) {
            sign = -1;
            coordinate = 0xffffffffl - coordinate;
        }

        double degrees = coordinate / 1000000;
        degrees += (coordinate % 1000000) / 600000.0;

        return sign * degrees;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Read header
        int apiNumber = buf.readUnsignedShort();
        int commandType = buf.readUnsignedByte();
        int messageType = buf.getUnsignedByte(buf.readerIndex()) >> 4;
        boolean needAck = (buf.readUnsignedByte() & 0xf) == 1;
        long mask = defaultMask;
        if (buf.readUnsignedByte() == 4) {
            mask = buf.readUnsignedInt();
        }

        // Binary position report
        if (apiNumber == 5 &&
            commandType == 2 &&
            messageType == 1 &&
            BitUtil.check(mask, 0)) {

            // Create new position
            Position position = new Position();
            position.setProtocol(getProtocolName());

            // Status code
            if (BitUtil.check(mask, 1)) {
                position.set(Event.KEY_STATUS, buf.readUnsignedInt());
            }

            // Device id
            String id = null;
            if (BitUtil.check(mask, 23)) {
                id = buf.toString(buf.readerIndex(), 8, Charset.defaultCharset()).trim();
                buf.skipBytes(8);
            } else if (BitUtil.check(mask, 2)) {
                id = buf.toString(buf.readerIndex(), 22, Charset.defaultCharset()).trim();
                buf.skipBytes(22);
            } else {
                Log.warning("No device id field");
                return null;
            }
            if (!identify(id, channel)) {
                return null;
            }
            position.setDeviceId(getDeviceId());

            // IO data
            if (BitUtil.check(mask, 3)) {
                buf.readUnsignedShort();
            }

            // ADC 1
            if (BitUtil.check(mask, 4)) {
                buf.readUnsignedShort();
            }

            // ADC 2
            if (BitUtil.check(mask, 5)) {
                buf.readUnsignedShort();
            }

            // Function category
            if (BitUtil.check(mask, 7)) {
                buf.readUnsignedByte();
            }

            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();

            // Date
            if (BitUtil.check(mask, 8)) {
                time.set(Calendar.DAY_OF_MONTH, buf.readUnsignedByte());
                time.set(Calendar.MONTH, buf.readUnsignedByte() - 1);
                time.set(Calendar.YEAR, 2000 + buf.readUnsignedByte());
            }

            // GPS status
            if (BitUtil.check(mask, 9)) {
                position.setValid(buf.readUnsignedByte() == 1);
            }

            // Latitude
            if (BitUtil.check(mask, 10)) {
                position.setLatitude(convertCoordinate(buf.readUnsignedInt()));
            }

            // Longitude
            if (BitUtil.check(mask, 11)) {
                position.setLongitude(convertCoordinate(buf.readUnsignedInt()));
            }

            // Speed
            if (BitUtil.check(mask, 12)) {
                position.setSpeed(buf.readUnsignedShort() / 10.0);
            }

            // Course
            if (BitUtil.check(mask, 13)) {
                position.setCourse(buf.readUnsignedShort() / 10.0);
            }

            // Time
            if (BitUtil.check(mask, 14)) {
                time.set(Calendar.HOUR_OF_DAY, buf.readUnsignedByte());
                time.set(Calendar.MINUTE, buf.readUnsignedByte());
                time.set(Calendar.SECOND, buf.readUnsignedByte());
            }

            position.setTime(time.getTime());

            // Altitude
            if (BitUtil.check(mask, 15)) {
                position.setAltitude(buf.readMedium());
            }

            // Satellites
            if (BitUtil.check(mask, 16)) {
                position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
            }

            // Battery percentage
            if (BitUtil.check(mask, 17)) {
                buf.readUnsignedShort();
            }

            // Trip odometer
            if (BitUtil.check(mask, 20)) {
                position.set("trip", buf.readUnsignedInt());
            }

            // Odometer
            if (BitUtil.check(mask, 21)) {
                position.set(Event.KEY_ODOMETER, buf.readUnsignedInt());
            }

            // Time of message generation
            if (BitUtil.check(mask, 22)) {
                buf.skipBytes(6);
            }

            // Battery level
            if (BitUtil.check(mask, 24)) {
                position.set(Event.KEY_POWER, buf.readUnsignedShort() / 1000.0);
            }

            // GPS overspeed
            if (BitUtil.check(mask, 25)) {
                buf.skipBytes(18);
            }

            // Cell information
            if (BitUtil.check(mask, 26)) {
                buf.skipBytes(54);
            }

            // Sequence number
            if (BitUtil.check(mask, 28)) {
                position.set(Event.KEY_INDEX, buf.readUnsignedShort());
            }

            return position;
        }

        return null;
    }

}
