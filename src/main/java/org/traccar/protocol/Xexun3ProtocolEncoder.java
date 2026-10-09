/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class Xexun3ProtocolEncoder extends BaseProtocolEncoder {

    public static final String KEY_PRIORITY = "priority";
    public static final String KEY_DELAY = "delay";
    public static final String KEY_STATIC_INTERVAL = "staticInterval";
    public static final String KEY_KEEPALIVE = "keepalive";

    public Xexun3ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(String uniqueId, String content) {
        ByteBuf buf = Unpooled.buffer();

        ByteBuf message = Unpooled.copiedBuffer(content.getBytes(StandardCharsets.US_ASCII));

        buf.writeByte(0xFC); // start
        int lengthIndex = buf.writerIndex();
        buf.writeShort(0); // length placeholder, back-patched after body is written
        buf.writeByte(0x03); // version
        buf.writeByte(Xexun3ProtocolDecoder.MSG_COMMAND); // type
        buf.writeByte(1); // index
        buf.writeBytes(DataConverter.parseHex("0" + uniqueId)); // imei
        buf.writeBytes(message); // body

        int length = buf.writerIndex() - lengthIndex - 2;
        buf.setShort(lengthIndex, length);

        buf.writeShort(Checksum.crc16(
                Checksum.CRC16_CCITT_FALSE, buf.nioBuffer(3, length)));
        buf.writeByte(0xCF); // end

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        String uniqueId = getUniqueId(command.getDeviceId());

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeContent(uniqueId, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC -> {
                int frequency = command.getInteger(Command.KEY_FREQUENCY);
                yield encodeContent(uniqueId, String.format("tk=%d,%d,%d,%d,%d",
                        command.getInteger(KEY_PRIORITY, 2),
                        frequency,
                        command.getInteger(KEY_DELAY, 0),
                        command.getInteger(KEY_STATIC_INTERVAL, frequency),
                        command.getInteger(KEY_KEEPALIVE, 1)));
            }
            case Command.TYPE_POWER_OFF -> encodeContent(uniqueId, "of=1");
            case Command.TYPE_REBOOT_DEVICE -> encodeContent(uniqueId, "rt=1");
            default -> null;
        };
    }

}
