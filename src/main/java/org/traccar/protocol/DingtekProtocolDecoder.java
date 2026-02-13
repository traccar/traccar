/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;

public class DingtekProtocolDecoder extends BaseProtocolDecoder {

    public DingtekProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        int type = Integer.parseInt(sentence.substring(6, 8), 16);

        if (type == 0x01 || type == 0x02 || type == 0x04) {

            ByteBuf buf = Unpooled.wrappedBuffer(DataConverter.parseHex(sentence));

            buf.readUnsignedByte(); // header
            buf.readUnsignedByte(); // forced
            buf.readUnsignedByte(); // device type
            buf.readUnsignedByte(); // type
            buf.readUnsignedByte(); // length

            String imei = ByteBufUtil.hexDump(buf.slice(buf.writerIndex() - 9, 8)).substring(1);
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setTime(new Date());

            position.set("height", buf.readUnsignedShort());

            position.setValid(buf.readUnsignedByte() > 0);
            position.setLongitude(buf.readFloat());
            position.setLatitude(buf.readFloat());

            position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedByte());
            position.set(Position.KEY_STATUS, buf.readUnsignedInt());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);
            position.set(Position.KEY_RSSI, buf.readFloat());
            position.set(Position.KEY_INDEX, buf.readUnsignedShort());

            return position;
        }

        return null;
    }

}
