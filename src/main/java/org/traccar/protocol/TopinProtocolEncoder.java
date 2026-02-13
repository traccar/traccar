/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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

public class TopinProtocolEncoder extends BaseProtocolEncoder {

    public TopinProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(int type, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x78);
        buf.writeByte(0x78);

        buf.writeByte(1 + content.readableBytes()); // message length

        buf.writeByte(type);

        buf.writeBytes(content);
        content.release();

        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf content = Unpooled.buffer();

        return switch (command.getType()) {
            case Command.TYPE_SOS_NUMBER -> {
                content.writeCharSequence(command.getString(Command.KEY_PHONE), StandardCharsets.US_ASCII);
                yield encodeContent(TopinProtocolDecoder.MSG_SOS_NUMBER, content);
            }
            default -> null;
        };
    }

}
