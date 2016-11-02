/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class GranitProtocolEncoder extends BaseProtocolEncoder {
    @Override
    protected Object encodeCommand(Command command) {

        String commandString = "";

        switch (command.getType()) {
            case Command.TYPE_IDENTIFICATION:
                commandString = "BB+IDNT";
                break;
            case Command.TYPE_REBOOT_DEVICE:
                commandString = "BB+RESET";
                break;
            case Command.TYPE_POSITION_SINGLE:
                commandString = "BB+RRCD";
                break;
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                return null;
        }
        if (!commandString.isEmpty()) {
            ChannelBuffer commandBuf = ChannelBuffers.dynamicBuffer();
            commandBuf.writeBytes(commandString.getBytes(StandardCharsets.US_ASCII));
            GranitProtocolDecoder.appendChecksum(commandBuf, commandString.length());
            return commandBuf;
        }
        return null;
    }

}
