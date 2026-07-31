/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Protocol;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class Itr120ProtocolEncoder extends BaseProtocolEncoder {

    private static final int PID_INSTRUCTION = 0x80;
    private static final int INSTRUCTION_TYPE_COMMAND = 0x01;

    public Itr120ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeInstruction(long sequence, String content) {
        byte[] data = content.getBytes(StandardCharsets.US_ASCII);
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x28);
        buf.writeByte(0x28);
        buf.writeByte(PID_INSTRUCTION);
        buf.writeShort(2 + 1 + 4 + data.length);
        buf.writeShort((int) sequence);
        buf.writeByte(INSTRUCTION_TYPE_COMMAND);
        buf.writeInt((int) sequence);
        buf.writeBytes(data);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        String content = switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP -> "RELAY,1#";
            case Command.TYPE_ENGINE_RESUME -> "RELAY,0#";
            case Command.TYPE_CUSTOM -> command.getString(Command.KEY_DATA);
            default -> null;
        };
        if (content == null) {
            return null;
        }
        long sequence = command.getDeviceId() & 0xffffL;
        return encodeInstruction(sequence != 0 ? sequence : 1, content);
    }

}
