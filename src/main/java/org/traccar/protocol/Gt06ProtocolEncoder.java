/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.Checksum;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class Gt06ProtocolEncoder extends BaseProtocolEncoder {

    public Gt06ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(long deviceId, String content) {

        boolean language = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_LANGUAGE.withPrefix(getProtocolName()), deviceId);

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x78);
        buf.writeByte(0x78);

        buf.writeByte(1 + 1 + 4 + content.length() + 2 + 2 + (language ? 2 : 0)); // message length

        buf.writeByte(0x80); // message type

        buf.writeByte(4 + content.length()); // command length
        buf.writeInt(0);
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII)); // command

        if (language) {
            buf.writeShort(2); // english language
        }

        buf.writeShort(0); // message index

        buf.writeShort(Checksum.crc16(Checksum.CRC16_X25, buf.nioBuffer(2, buf.writerIndex() - 2)));

        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        String password = AttributeUtil.getDevicePassword(
                getCacheManager(), command.getDeviceId(), getProtocolName(), "123456");

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(),
                    alternative ? "DYD," + password + "#" : "Relay,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(),
                    alternative ? "HFYD," + password + "#" : "Relay,0#");
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            default:
                return null;
        }
    }

}
