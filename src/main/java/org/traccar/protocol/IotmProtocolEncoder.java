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
import io.netty.handler.codec.mqtt.MqttQoS;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

import java.util.concurrent.atomic.AtomicInteger;

public class IotmProtocolEncoder extends BaseProtocolEncoder {

    private static final int OUTPUT_ID_STATIC_SIGNAL = 0x08;

    private final AtomicInteger messageId = new AtomicInteger();
    private final AtomicInteger commandIndex = new AtomicInteger();
    private final boolean permanentOutputControl;

    public IotmProtocolEncoder(Protocol protocol, Config config) {
        super(protocol);
        permanentOutputControl = config.getBoolean(Keys.IOTM_PERMANENT_OUTPUT_CONTROL);
    }

    @Override
    protected Object encodeCommand(Command command) {

        if (!command.getType().equals(Command.TYPE_OUTPUT_CONTROL)) {
            return null;
        }

        String uniqueId = getUniqueId(command.getDeviceId());

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(2); // structure version

        buf.writeByte(2); // record type imei
        buf.writeShortLE(8); // record length
        buf.writeLongLE(Long.parseLong(uniqueId));

        buf.writeByte(4); // record type output control
        buf.writeShortLE(permanentOutputControl ? 8 : 10); // record length
        buf.writeIntLE(Integer.MAX_VALUE); // expiration
        int index = command.getInteger(Command.KEY_INDEX);
        if (permanentOutputControl) {
            if (index < 1 || index > 2) {
                throw new IllegalArgumentException("Unsupported permanent output index");
            }
            buf.writeByte(OUTPUT_ID_STATIC_SIGNAL + index - 1); // output id
        } else {
            buf.writeByte(index - 1); // output id
        }
        buf.writeByte(commandIndex.updateAndGet(value -> value >= 0xff ? 1 : value + 1));
        int data = command.getInteger(Command.KEY_DATA);
        if (permanentOutputControl) {
            if (data != 0 && data != 1) {
                throw new IllegalArgumentException("Unsupported permanent output value");
            }
            buf.writeByte(1); // length
            buf.writeByte(data);
        } else {
            buf.writeByte(3); // length
            buf.writeByte(data);
            buf.writeByte(0xB0);
            buf.writeByte(0xB1);
        }

        buf.writeByte(Checksum.sum(buf.nioBuffer()));

        return MqttMessageBuilders.publish()
                .topicName(uniqueId + "/OUTC")
                .qos(MqttQoS.AT_LEAST_ONCE)
                .payload(buf)
                .messageId(messageId.updateAndGet(value -> value == 0xffff ? 1 : value + 1))
                .build();
    }

}
