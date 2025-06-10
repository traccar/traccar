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

import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class TeltonikaProtocolEncoder extends BaseProtocolEncoder {

    public TeltonikaProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(byte[] content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(0);
        buf.writeInt(content.length + 8);
        buf.writeByte(TeltonikaProtocolDecoder.CODEC_12);
        buf.writeByte(1); // quantity
        buf.writeByte(5); // type
        buf.writeInt(content.length);
        buf.writeBytes(content);
        buf.writeByte(1); // quantity
        buf.writeInt(Checksum.crc16(Checksum.CRC16_IBM, buf.nioBuffer(8, buf.writerIndex() - 8)));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP -> encodeContent("setdigout 1\r\n".getBytes(StandardCharsets.US_ASCII));
            case Command.TYPE_ENGINE_RESUME -> encodeContent("setdigout 0\r\n".getBytes(StandardCharsets.US_ASCII));
            case Command.TYPE_CUSTOM -> {
                String data = command.getString(Command.KEY_DATA);
                if (data.matches("(\\p{XDigit}{2})+")) {
                    yield encodeContent(DataConverter.parseHex(data));
                } else {
                    yield encodeContent((data + "\r\n").getBytes(StandardCharsets.US_ASCII));
                }
            }
            default -> null;
        };
    }

}
