/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class BceProtocolEncoder extends BaseProtocolEncoder {

    public BceProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        if (command.getType().equals(Command.TYPE_OUTPUT_CONTROL)) {
            ByteBuf buf = Unpooled.buffer();

            buf.writeLongLE(Long.parseLong(getUniqueId(command.getDeviceId())));
            buf.writeShortLE(1 + 1 + 3 + 1); // length
            buf.writeByte(BceProtocolDecoder.MSG_OUTPUT_CONTROL);
            buf.writeByte(command.getInteger(Command.KEY_INDEX) == 1 ? 0x0A : 0x0B);
            buf.writeByte(0xFF); // index
            buf.writeByte(0x00); // form id
            buf.writeShortLE(Integer.parseInt(command.getString(Command.KEY_DATA)) > 0 ? 0x0055 : 0x0000);
            buf.writeByte(Checksum.sum(buf.nioBuffer()));

            return buf;
        } else {
            return null;
        }
    }

}
