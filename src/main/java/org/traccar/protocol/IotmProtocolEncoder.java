/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

public class IotmProtocolEncoder extends BaseProtocolEncoder {

    public IotmProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        if (!command.getType().equals(Command.TYPE_OUTPUT_CONTROL)) {
            return null;
        }

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(2); // structure version

        buf.writeByte(2); // record type imei
        buf.writeShortLE(8); // record length
        buf.writeLongLE(Long.parseLong(getUniqueId(command.getDeviceId())));

        buf.writeByte(4); // record type output control
        buf.writeShortLE(10); // record length
        buf.writeIntLE(Integer.MAX_VALUE); // expiration
        buf.writeByte(command.getInteger(Command.KEY_INDEX) - 1); // output id
        buf.writeByte(0); // output command index
        buf.writeByte(3); // length
        buf.writeByte(command.getInteger(Command.KEY_DATA));
        buf.writeByte(0xB0);
        buf.writeByte(0xB1);

        buf.writeByte(Checksum.sum(buf.nioBuffer()));

        return MqttMessageBuilders.publish().payload(buf).build();
    }

}
