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
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EelinkProtocolEncoder extends BaseProtocolEncoder {

    private final boolean connectionless;
    private int commandId;

    public EelinkProtocolEncoder(Protocol protocol, boolean connectionless) {
        super(protocol);
        this.connectionless = connectionless;
    }

    public static int checksum(ByteBuffer buf) {
        int sum = 0;
        while (buf.hasRemaining()) {
            sum = (((sum << 1) | (sum >> 15)) + (buf.get() & 0xFF)) & 0xFFFF;
        }
        return sum;
    }

    public static ByteBuf encodeContent(
            boolean connectionless, int header, String uniqueId, int type, int index, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        if (connectionless) {
            buf.writeBytes(DataConverter.parseHex('0' + uniqueId));
        }

        buf.writeShort(header);
        buf.writeByte(type);
        buf.writeShort(2 + (content != null ? content.readableBytes() : 0)); // length
        buf.writeShort(index);

        if (content != null) {
            buf.writeBytes(content);
        }

        ByteBuf result = Unpooled.buffer();

        if (connectionless) {
            result.writeByte('E');
            result.writeByte('L');
            result.writeShort(2 + buf.readableBytes()); // length
            result.writeShort(checksum(buf.nioBuffer()));
        }

        result.writeBytes(buf);
        buf.release();

        return result;
    }

    private ByteBuf encodeContent(Channel channel, Command command, String content) {

        EelinkProtocolDecoder decoder = channel.pipeline().get(EelinkProtocolDecoder.class);
        int header = decoder.getHeader(command.getDeviceId());
        boolean iter = header == EelinkProtocolDecoder.HEADER_ITER;

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x01); // command
        buf.writeInt(iter ? ++commandId : 0); // command id or server id
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        return encodeContent(
                connectionless, header, getUniqueId(command.getDeviceId()),
                EelinkProtocolDecoder.MSG_DOWNLINK, iter ? 1 : 0, buf);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM ->
                    encodeContent(channel, command, command.getString(Command.KEY_DATA));
            case Command.TYPE_CONFIGURATION ->
                    encodeContent(channel, command, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE -> encodeContent(channel, command, "WHERE#");
            case Command.TYPE_ENGINE_STOP -> encodeContent(channel, command, "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME -> encodeContent(channel, command, "RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE -> encodeContent(channel, command, "RESET#");
            case Command.TYPE_GET_VERSION -> encodeContent(channel, command, "VERSION?");
            case Command.TYPE_GET_DEVICE_STATUS -> encodeContent(channel, command, "STATUS?");
            case Command.TYPE_IDENTIFICATION -> encodeContent(channel, command, "PARAM?");
            default -> null;
        };
    }

}
