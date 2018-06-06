/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.model.Command;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class TeltonikaProtocolEncoder extends BaseProtocolEncoder {

    private ByteBuf encodeContent(String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(0);
        buf.writeInt(content.length() + 10);
        buf.writeByte(TeltonikaProtocolDecoder.CODEC_12);
        buf.writeByte(1); // quantity
        buf.writeByte(5); // type
        buf.writeInt(content.length() + 2);
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte('\r');
        buf.writeByte('\n');
        buf.writeByte(1); // quantity
        buf.writeInt(Checksum.crc16(Checksum.CRC16_IBM, buf.nioBuffer(8, buf.writerIndex() - 8)));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getString(Command.KEY_DATA));
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
