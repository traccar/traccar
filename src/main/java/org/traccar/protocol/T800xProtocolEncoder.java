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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolEncoder;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;

public class T800xProtocolEncoder extends BaseProtocolEncoder {

    public static final int MODE_SETTING = 0x01;
    public static final int MODE_BROADCAST = 0x02;
    public static final int MODE_FORWARD = 0x03;

    public T800xProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(Command command, short header, String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(header);
        buf.writeByte(T800xProtocolDecoder.MSG_COMMAND);
        buf.writeShort(7 + 8 + 1 + content.length());
        buf.writeShort(1); // serial number
        buf.writeBytes(DataConverter.parseHex("0" + getUniqueId(command.getDeviceId())));
        buf.writeByte(MODE_SETTING);
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));

        return buf;
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        short header = T800xProtocolDecoder.DEFAULT_HEADER;
        if (channel != null) {
            header = channel.pipeline().get(T800xProtocolDecoder.class).getHeader();
        }

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeContent(command, header, command.getString(Command.KEY_DATA));
            default -> null;
        };
    }

}
