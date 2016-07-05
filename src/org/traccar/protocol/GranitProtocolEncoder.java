/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class GranitProtocolEncoder extends BaseProtocolEncoder {
    @Override
    protected Object encodeCommand(Command command) {

        ChannelBuffer commandString;

        switch (command.getType()) {
        case Command.TYPE_IDENTIFICATION:
            commandString = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 12);
            commandString.writeBytes("BB+IDNT".getBytes(StandardCharsets.US_ASCII));
            GranitProtocolDecoder.appendChecksum(commandString, 7);
            return commandString;
        case Command.TYPE_REBOOT_DEVICE:
            commandString = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 13);
            commandString.writeBytes("BB+RESET".getBytes(StandardCharsets.US_ASCII));
            GranitProtocolDecoder.appendChecksum(commandString, 8);
            return commandString;
        case Command.TYPE_POSITION_SINGLE:
            commandString = ChannelBuffers.directBuffer(ByteOrder.LITTLE_ENDIAN, 12);
            commandString.writeBytes("BB+RRCD".getBytes(StandardCharsets.US_ASCII));
            GranitProtocolDecoder.appendChecksum(commandString, 7);
            return commandString;
        default:
            Log.warning(new UnsupportedOperationException(command.getType()));
            break;
        }

        return null;
    }

}
