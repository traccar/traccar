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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class MotorProtocolDecoder extends BaseProtocolDecoder {

    public MotorProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(sentence));

        String id = String.format("%08x", buf.readUnsignedIntLE());
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, id);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.skipBytes(2); // divider

        position.set(Position.KEY_STATUS, buf.readUnsignedShortLE());

        buf.skipBytes(2); // divider
        buf.readUnsignedMediumLE(); // command

        int flags = buf.readUnsignedByte();
        position.setValid(BitUtil.check(flags, 7));
        if (BitUtil.check(flags, 0)) {
            position.addAlarm(Position.ALARM_GENERAL);
        }

        position.setLatitude(BcdUtil.readInteger(buf, 2) + BcdUtil.readInteger(buf, 6) * 0.0001 / 60);
        position.setLongitude(BcdUtil.readInteger(buf, 4) + BcdUtil.readInteger(buf, 6) * 0.0001 / 60);
        position.setSpeed(BcdUtil.readInteger(buf, 4) * 0.1);
        position.setCourse(BcdUtil.readInteger(buf, 4) * 0.1);

        position.setTime(new DateBuilder()
                .setYear(BcdUtil.readInteger(buf, 2))
                .setMonth(BcdUtil.readInteger(buf, 2))
                .setDay(BcdUtil.readInteger(buf, 2))
                .setHour(BcdUtil.readInteger(buf, 2))
                .setMinute(BcdUtil.readInteger(buf, 2))
                .setSecond(BcdUtil.readInteger(buf, 2)).getDate());

        position.set(Position.KEY_RSSI, BcdUtil.readInteger(buf, 2));

        return position;
    }

}
