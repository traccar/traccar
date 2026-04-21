/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Jose Castellanos
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
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;

public class EnforaProtocolEncoder extends StringProtocolEncoder {

    public EnforaProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(content.length() + 6);
        buf.writeShort(0); // index
        buf.writeByte(0x04); // command type
        buf.writeByte(0); // optional header
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeContent(command.getString(Command.KEY_DATA));
            case Command.TYPE_ENGINE_STOP -> encodeContent("AT$IOGP3=1");
            case Command.TYPE_ENGINE_RESUME -> encodeContent("AT$IOGP3=0");
            default -> null;
        };
    }

}
