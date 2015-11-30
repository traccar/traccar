/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.util.Arrays;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;
import static org.traccar.protocol.AutoFon45FrameDecoder.MSG_LOCATION;
import static org.traccar.protocol.AutoFon45FrameDecoder.MSG_LOGIN;

public class AutoFon45ProtocolDecoder extends BaseProtocolDecoder {

    public AutoFon45ProtocolDecoder(AutoFon45Protocol protocol) {
        super(protocol);
    }

    private static double convertCoordinate(short degrees, int minutes) {
        double value = degrees + BitUtil.from(minutes, 4) / 600000.0;
        if (BitUtil.check(minutes, 0)) {
            return value;
        } else {
            return -value;
        }
    }

    private static byte checksum(byte[] bytes, int offset, int len) {
        byte result = 0x3B;
        for (int i = offset; i < offset + len; i++) {
            result += 0x56 ^ bytes[i];
            result++;
            result ^= 0xC5 + bytes[i];
            result--;
        }
        return result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        int type = buf.getUnsignedByte(buf.readerIndex());

        if (type == MSG_LOGIN) {

            byte[] bytes = new byte[19];
            buf.readBytes(bytes);

            String imei = ChannelBuffers.hexDump(ChannelBuffers.wrappedBuffer(bytes, 1, 8)).substring(1);
            if (!identify(imei, channel, remoteAddress)) {
                return null;
            }

            // Send response (checksum)
            if (channel != null) {
                byte[] response = "resp_crc=".getBytes("US-ASCII");
                response = Arrays.copyOf(response, response.length + 1);
                response[response.length - 1] = checksum(bytes, 0, 18);
                channel.write(ChannelBuffers.wrappedBuffer(response));
            }

        } else if (type == MSG_LOCATION) {

            buf.readUnsignedByte();

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            short status = buf.readUnsignedByte();
            position.set(Event.KEY_ALARM, BitUtil.check(status, 7));
            position.set(Event.KEY_BATTERY, BitUtil.to(status, 7));

            buf.skipBytes(2); // remaining time

            position.set(Event.PREFIX_TEMP + 1, buf.readByte());

            buf.skipBytes(2); // timer (interval and units)
            buf.readByte(); // mode
            buf.readByte(); // gprs sending interval

            buf.skipBytes(6); // mcc, mnc, lac, cid

            int valid = buf.readUnsignedByte();
            position.setValid(BitUtil.from(valid, 6) != 0);
            position.set(Event.KEY_SATELLITES, BitUtil.from(valid, 6));

            int time = buf.readUnsignedMedium();
            int date = buf.readUnsignedMedium();

            DateBuilder dateBuilder = new DateBuilder()
                    .setTime(time / 10000, time / 100 % 100, time % 100)
                    .setDateReverse(date / 10000, date / 100 % 100, date % 100);
            position.setTime(dateBuilder.getDate());

            position.setLatitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedMedium()));
            position.setLongitude(convertCoordinate(buf.readUnsignedByte(), buf.readUnsignedMedium()));
            position.setSpeed(buf.readUnsignedByte());
            position.setCourse(buf.readUnsignedShort());

            return position;

        }

        return null;
    }

}
