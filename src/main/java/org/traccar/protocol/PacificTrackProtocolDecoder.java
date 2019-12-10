/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class PacificTrackProtocolDecoder extends BaseProtocolDecoder {

    public PacificTrackProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static int readBitExt(ByteBuf buf) {
        int result = 0;
        while (buf.isReadable()) {
            int b = buf.readUnsignedByte();
            result <<= 7;
            result += BitUtil.to(b, 7);
            if (BitUtil.check(b, 7)) {
                break;
            }
        }
        return result;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readByte(); // frame start
        readBitExt(buf); // frame control
        readBitExt(buf); // frame length

        DeviceSession deviceSession = null;
        Position position = new Position(getProtocolName());

        while (buf.isReadable()) {

            int segmentId = readBitExt(buf);
            int segmentLength = readBitExt(buf);

            switch (segmentId) {
                case 0x01:
                    position.set(Position.KEY_EVENT, readBitExt(buf));
                    break;
                case 0x10:
                    position.setValid(BitUtil.check(buf.readUnsignedByte(), 4));
                    int date = buf.readUnsignedByte();
                    DateBuilder dateBuilder = new DateBuilder()
                            .setDate(2010 + BitUtil.from(date, 4), BitUtil.to(date, 4), buf.readUnsignedByte())
                            .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                    position.setTime(dateBuilder.getDate());
                    position.setLatitude(buf.readUnsignedInt() / 1000000.0 - 90.0);
                    position.setLongitude(buf.readUnsignedInt() / 1000000.0 - 180.0);
                    int speedAndCourse = buf.readUnsignedMedium();
                    position.setCourse(BitUtil.from(speedAndCourse, 12));
                    position.setSpeed(UnitsConverter.knotsFromKph(BitUtil.to(speedAndCourse, 12) * 0.1));
                    position.set(Position.KEY_INDEX, buf.readUnsignedShort());
                    break;
                case 0x100:
                    String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(0, 15);
                    deviceSession = getDeviceSession(channel, remoteAddress, imei);
                    break;
                default:
                    buf.skipBytes(segmentLength);
                    break;
            }
        }

        if (deviceSession != null) {
            position.setDeviceId(deviceSession.getDeviceId());
            return position;
        } else {
            return null;
        }
    }

}
