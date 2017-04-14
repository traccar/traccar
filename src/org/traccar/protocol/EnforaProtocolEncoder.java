/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class EnforaProtocolEncoder extends StringProtocolEncoder {

    private ChannelBuffer encodeContent(String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeShort(content.length() + 6);
        buf.writeShort(0); // index
        buf.writeByte(0x04); // command type
        buf.writeByte(0); // optional header
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getString(Command.KEY_DATA));
            case Command.TYPE_ENGINE_STOP:
                return encodeContent("AT$IOGP3=1");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent("AT$IOGP3=0");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
