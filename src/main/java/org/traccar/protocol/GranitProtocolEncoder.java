/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class GranitProtocolEncoder extends BaseProtocolEncoder {

    public GranitProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommand(String commandString) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(commandString.getBytes(StandardCharsets.US_ASCII));
        GranitProtocolDecoder.appendChecksum(buffer, commandString.length());
        return buffer;
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_IDENTIFICATION -> encodeCommand("BB+IDNT");
            case Command.TYPE_REBOOT_DEVICE -> encodeCommand("BB+RESET");
            case Command.TYPE_POSITION_SINGLE -> encodeCommand("BB+RRCD");
            default -> null;
        };
    }

}
