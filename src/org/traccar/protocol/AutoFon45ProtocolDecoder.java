/*
 * Copyright 2015 Vitaly Litvak (vitavaque@gmail.com)
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

import static org.traccar.protocol.AutoFon45FrameDecoder.MSG_LOGIN;
import static org.traccar.protocol.AutoFon45FrameDecoder.MSG_LOCATION;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.*;

public class AutoFon45ProtocolDecoder extends BaseProtocolDecoder {

    private static double convertCoordinate(short degrees, int raw) {
        double seconds = (raw >> 4 & 0xffffff) / 600000.0;
        return (degrees + seconds) * ((raw & 0x0f) == 0 ? -1 : 1);
    }

    public AutoFon45ProtocolDecoder(AutoFon45Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        ChannelBuffer buf = (ChannelBuffer) msg;

        int type = buf.getUnsignedByte(0);

        if (type == MSG_LOGIN) {
            byte[] bytes = new byte[19];
            buf.readBytes(bytes);

            String imei = ChannelBufferTools.readHexString(ChannelBuffers.wrappedBuffer(bytes, 1, 16), 16).substring(1);
            if (!identify(imei, channel)) {
                return null;
            }

            // Send response (CRC)
            if (channel != null) {
                byte[] response = "resp_crc=".getBytes("US-ASCII");
                response = Arrays.copyOf(response, response.length + 1);
                response[response.length - 1] = crc(bytes, 0, 18);
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }
        } else if (type == MSG_LOCATION) {
            buf.readUnsignedByte();

            // Create new position
            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            short status = buf.readUnsignedByte();
            position.set(Event.KEY_ALARM, (status & 0x80) != 0);
            position.set(Event.KEY_BATTERY, status & 0x7F);

            buf.skipBytes(2); // remaining time

            position.set(Event.PREFIX_TEMP + 1, buf.readByte());

            buf.skipBytes(2); // timer (interval and units)
            buf.readByte(); // mode
            buf.readByte(); // gprs sending interval

            buf.skipBytes(6); // MCC, MNC, LAC, CID

            // GPS status
            int valid = buf.readUnsignedByte();
            position.setValid((valid & 0xc0) != 0);
            position.set(Event.KEY_SATELLITES, valid & 0x3f);

            // Date and time
            int timeOfDay = buf.readUnsignedByte() << 16 | buf.readUnsignedByte() << 8 | buf.readUnsignedByte();
            int date = buf.readUnsignedByte() << 16 | buf.readUnsignedByte() << 8 | buf.readUnsignedByte();

            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.HOUR_OF_DAY, timeOfDay / 10000);
            time.set(Calendar.MINUTE, (timeOfDay - time.get(Calendar.HOUR_OF_DAY) * 10000) / 100);
            time.set(Calendar.SECOND, (timeOfDay - time.get(Calendar.HOUR_OF_DAY) * 10000 - time.get(Calendar.MINUTE) * 100));
            time.set(Calendar.DAY_OF_MONTH, date / 10000);
            time.set(Calendar.MONTH, (date - time.get(Calendar.DAY_OF_MONTH) * 10000) / 100 - 1);
            time.set(Calendar.YEAR, 2000 + (date - time.get(Calendar.DAY_OF_MONTH) * 10000 - (time.get(Calendar.MONTH) + 1) * 100));
            position.setTime(time.getTime());

            // Location
            position.setLatitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedByte() << 16 | buf.readUnsignedByte() << 8 | buf.readUnsignedByte()));
            position.setLongitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedByte() << 16 | buf.readUnsignedByte() << 8 | buf.readUnsignedByte()));
            position.setSpeed(buf.readUnsignedByte());
            position.setCourse(buf.readUnsignedByte() << 8 | buf.readUnsignedByte());

            buf.readUnsignedByte(); // checksum
            return position;
        }

        return null;
    }

    private byte crc(byte[] bytes, int offset, int len) {
        byte GPRS_CRC = 0x3B;
        for (int i = offset; i < offset + len; i++) {
            GPRS_CRC += 0x56 ^ bytes[i];
            GPRS_CRC++;
            GPRS_CRC ^= 0xC5 + bytes[i];
            GPRS_CRC--;
        }
        return GPRS_CRC;
    }
}
