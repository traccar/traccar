/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class KhdProtocolEncoder extends BaseProtocolEncoder {

    public static final int MSG_CUT_OIL = 0x39;
    public static final int MSG_RESUME_OIL = 0x38;

    private ChannelBuffer encodeCommand(int command) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte(0x29);
        buf.writeByte(0x29);

        buf.writeByte(command);
        buf.writeShort(6); // size

        buf.writeInt(0); // terminal id

        buf.writeByte(Checksum.xor(buf.toByteBuffer()));
        buf.writeByte(0x0D); // ending

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeCommand(MSG_CUT_OIL);
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommand(MSG_RESUME_OIL);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
