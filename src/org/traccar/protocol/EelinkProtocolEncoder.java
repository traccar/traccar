/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Log;
import org.traccar.model.Command;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EelinkProtocolEncoder extends BaseProtocolEncoder {

    private boolean connectionless;

    public EelinkProtocolEncoder(boolean connectionless) {
        this.connectionless = connectionless;
    }

    public static int checksum(ByteBuffer buf) {
        int sum = 0;
        while (buf.hasRemaining()) {
            sum = (((sum << 1) | (sum >> 15)) + (buf.get() & 0xFF)) & 0xFFFF;
        }
        return sum;
    }

    public static ChannelBuffer encodeContent(
            boolean connectionless, String uniqueId, int type, int index, ChannelBuffer content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        if (connectionless) {
            buf.writeBytes(ChannelBuffers.wrappedBuffer(DatatypeConverter.parseHexBinary('0' + uniqueId)));
        }

        buf.writeByte(0x67);
        buf.writeByte(0x67);
        buf.writeByte(type);
        buf.writeShort(2 + (content != null ? content.readableBytes() : 0)); // length
        buf.writeShort(index);

        if (content != null) {
            buf.writeBytes(content);
        }

        ChannelBuffer result = ChannelBuffers.dynamicBuffer();

        if (connectionless) {
            result.writeByte('E');
            result.writeByte('L');
            result.writeShort(2 + buf.readableBytes()); // length
            result.writeShort(checksum(buf.toByteBuffer()));
        }

        result.writeBytes(buf);

        return result;
    }

    private ChannelBuffer encodeContent(long deviceId, String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte(0x01); // command
        buf.writeInt(0); // server id
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        return encodeContent(connectionless, getUniqueId(deviceId), EelinkProtocolDecoder.MSG_DOWNLINK, 0, buf);
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), "WHERE#");
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(), "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(), "RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(command.getDeviceId(), "RESET#");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
