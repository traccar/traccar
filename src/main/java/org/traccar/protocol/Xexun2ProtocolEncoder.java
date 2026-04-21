/*
 * Copyright 2022 Stefan Clark (stefan@stefanclark.co.uk)
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
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;

public class Xexun2ProtocolEncoder extends BaseProtocolEncoder {

    public Xexun2ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private static ByteBuf encodeContent(String uniqueId, String content) {
        ByteBuf buf = Unpooled.buffer();

        ByteBuf message = Unpooled.copiedBuffer(content.getBytes(StandardCharsets.US_ASCII));

        buf.writeShort(Xexun2ProtocolDecoder.FLAG);
        buf.writeShort(Xexun2ProtocolDecoder.MSG_COMMAND);
        buf.writeShort(1); // index
        buf.writeBytes(DataConverter.parseHex(uniqueId + "0"));
        buf.writeShort(message.readableBytes());
        buf.writeShort(Checksum.ip(message.nioBuffer()));
        buf.writeBytes(message);
        buf.writeShort(Xexun2ProtocolDecoder.FLAG);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        String uniqueId = getUniqueId(command.getDeviceId());

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeContent(uniqueId, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC -> encodeContent(
                    uniqueId, String.format("tracking_send=%1$d,%1$d", command.getInteger(Command.KEY_FREQUENCY)));
            case Command.TYPE_POWER_OFF -> encodeContent(uniqueId, "of=1");
            case Command.TYPE_REBOOT_DEVICE -> encodeContent(uniqueId, "reset");
            default -> null;
        };
    }

}
