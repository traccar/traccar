/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class AutoFonProtocolDecoder extends BaseProtocolDecoder {

    public AutoFonProtocolDecoder(AutoFonProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_LOGIN = 0x10;
    public static final int MSG_LOCATION = 0x11;
    public static final int MSG_HISTORY = 0x12;

    public static final int MSG_45_LOGIN = 0x41;
    public static final int MSG_45_LOCATION = 0x02;

    private static double convertCoordinate(int raw) {
        int degrees = raw / 1000000;
        double minutes = (raw % 1000000) / 10000.0;
        return degrees + minutes / 60;
    }

    private static double convertCoordinate(short degrees, int minutes) {
        double value = degrees + BitUtil.from(minutes, 4) / 600000.0;
        if (BitUtil.check(minutes, 0)) {
            return value;
        } else {
            return -value;
        }
    }

    private Position decodePosition(ChannelBuffer buf, boolean history) {

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        if (!history) {
            buf.readUnsignedByte(); // interval
            buf.skipBytes(8); // settings
        }
        buf.readUnsignedByte(); // status
        if (!history) {
            buf.readUnsignedShort();
        }
        position.set(Event.KEY_BATTERY, buf.readUnsignedByte());
        buf.skipBytes(6); // time

        if (!history) {
            for (int i = 0; i < 2; i++) {
                buf.skipBytes(5); // time
                buf.readUnsignedShort(); // interval
                buf.skipBytes(5); // mode
            }
        }

        position.set(Event.PREFIX_TEMP + 1, buf.readByte());
        position.set(Event.KEY_GSM, buf.readUnsignedByte());
        buf.readUnsignedShort(); // mcc
        buf.readUnsignedShort(); // mnc
        buf.readUnsignedShort(); // lac
        buf.readUnsignedShort(); // cid

        int valid = buf.readUnsignedByte();
        position.setValid((valid & 0xc0) != 0);
        position.set(Event.KEY_SATELLITES, valid & 0x3f);

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(convertCoordinate(buf.readInt()));
        position.setLongitude(convertCoordinate(buf.readInt()));
        position.setAltitude(buf.readShort());
        position.setSpeed(buf.readUnsignedByte());
        position.setCourse(buf.readUnsignedByte() * 2.0);

        position.set(Event.KEY_HDOP, buf.readUnsignedShort());

        buf.readUnsignedShort(); // reserved
        buf.readUnsignedByte(); // checksum
        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        int type = buf.readUnsignedByte();

        if (type == MSG_LOGIN || type == MSG_45_LOGIN) {

            if (type == MSG_LOGIN) {
                buf.readUnsignedByte(); // hardware version
                buf.readUnsignedByte(); // software version
            }

            String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
            if (!identify(imei, channel, remoteAddress)) {
                return null;
            }

            if (channel != null) {
                ChannelBuffer response = ChannelBuffers.dynamicBuffer();
                response.writeBytes("resp_crc=".getBytes(StandardCharsets.US_ASCII));
                response.writeByte(buf.getByte(buf.writerIndex() - 1));
                channel.write(response);
            }

        } else if (type == MSG_LOCATION) {

            return decodePosition(buf, false);

        } else if (type == MSG_HISTORY) {

            int count = buf.readUnsignedByte() & 0x0f;
            buf.readUnsignedShort(); // total count
            List<Position> positions = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                positions.add(decodePosition(buf, true));
            }

            return positions;

        } else if (type == MSG_45_LOCATION) {

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
