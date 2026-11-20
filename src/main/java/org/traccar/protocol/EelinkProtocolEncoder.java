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
import io.netty.channel.socket.DatagramChannel;
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
            Channel channel, String uniqueId, int type, int index, ByteBuf content) {

        EelinkProtocolDecoder decoder = channel.pipeline().get(EelinkProtocolDecoder.class);
        return encodeContent(
                channel instanceof DatagramChannel, decoder.getHeader(), uniqueId, type, index, content);
    }

    private static ByteBuf encodeContent(
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

    private ByteBuf encodeContent(long deviceId, int header, String content) {

        ByteBuf buf = Unpooled.buffer();

        boolean iter = header == EelinkProtocolDecoder.HEADER_ITER;
        buf.writeByte(0x01); // command
        buf.writeInt(iter ? ++commandId : 0); // command id or server id
        buf.writeBytes(content.getBytes(iter ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8));

        return encodeContent(
                connectionless, header, getUniqueId(deviceId), EelinkProtocolDecoder.MSG_DOWNLINK, iter ? 1 : 0, buf);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        EelinkProtocolDecoder decoder = channel != null
                ? channel.pipeline().get(EelinkProtocolDecoder.class) : null;
        int header = EelinkProtocolDecoder.HEADER_EELINK;
        if (decoder != null) {
            header = connectionless ? decoder.getUdpHeader(command.getDeviceId()) : decoder.getHeader();
        }

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM ->
                    encodeContent(command.getDeviceId(), header, command.getString(Command.KEY_DATA));
            case Command.TYPE_CONFIGURATION ->
                    encodeContent(command.getDeviceId(), header, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE -> encodeContent(command.getDeviceId(), header,
                    header == EelinkProtocolDecoder.HEADER_ITER ? "PAGING#" : "WHERE#");
            case Command.TYPE_ENGINE_STOP -> encodeContent(command.getDeviceId(), header, "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME -> encodeContent(command.getDeviceId(), header, "RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE -> encodeContent(command.getDeviceId(), header, "RESET#");
            case Command.TYPE_GET_VERSION -> encodeContent(command.getDeviceId(), header, "VERSION?");
            case Command.TYPE_GET_DEVICE_STATUS -> encodeContent(command.getDeviceId(), header, "STATUS?");
            case Command.TYPE_IDENTIFICATION -> encodeContent(command.getDeviceId(), header, "PARAM?");
            default -> null;
        };
    }

}
