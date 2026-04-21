/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;

public class GalileoProtocolEncoder extends BaseProtocolEncoder {

    public GalileoProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeText(String uniqueId, String text) {

        ByteBuf buf = Unpooled.buffer(256);

        buf.writeByte(0x01);
        buf.writeShortLE(uniqueId.length() + text.length() + 11);

        buf.writeByte(0x03); // imei tag
        buf.writeBytes(uniqueId.getBytes(StandardCharsets.US_ASCII));

        buf.writeByte(0x04); // device id tag
        buf.writeShortLE(0); // not needed if imei provided

        buf.writeByte(0xE0); // index tag
        buf.writeIntLE(0); // index

        buf.writeByte(0xE1); // command text tag
        buf.writeByte(text.length());
        buf.writeBytes(text.getBytes(StandardCharsets.US_ASCII));

        buf.writeShortLE(Checksum.crc16(Checksum.CRC16_MODBUS, buf.nioBuffer(0, buf.writerIndex())));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeText(getUniqueId(command.getDeviceId()),
                    command.getString(Command.KEY_DATA));
            case Command.TYPE_OUTPUT_CONTROL -> encodeText(getUniqueId(command.getDeviceId()),
                    "Out " + command.getInteger(Command.KEY_INDEX) + "," + command.getString(Command.KEY_DATA));
            default -> null;
        };
    }

}
