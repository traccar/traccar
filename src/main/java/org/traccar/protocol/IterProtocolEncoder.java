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
import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class IterProtocolEncoder extends StringProtocolEncoder {

    public IterProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private int uid;

    private ByteBuf encodeContent(String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(0x2828); // mark
        buf.writeByte(0x80); // instruction package
        byte[] data = content.getBytes(StandardCharsets.US_ASCII);
        buf.writeShort(2 + 1 + 4 + data.length); // sequence + type + uid + content
        buf.writeShort(1); // sequence
        buf.writeByte(0x01); // device command
        buf.writeInt(++uid);
        buf.writeBytes(data);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getString(Command.KEY_DATA));
            case Command.TYPE_ENGINE_STOP:
                return encodeContent("RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent("RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent("RESET#");
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent("PAGING#");
            case Command.TYPE_GET_VERSION:
                return encodeContent("VERSION?");
            default:
                return null;
        }
    }

}
