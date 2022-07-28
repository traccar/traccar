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

public class Xexun2ProtocolEncoder extends BaseProtocolEncoder {

    public Xexun2ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static final int FLAG = 0xfaaf;
    public static final int MSG_COMMAND = 0x07;

    private static ByteBuf encodeFrame(ByteBuf buf) {
        int bufLength = buf.readableBytes();
        if (bufLength < 5) {
            return null;
        }

        ByteBuf result = Unpooled.buffer();

        result.writeBytes(buf.readBytes(2));

        while (buf.readerIndex() < bufLength - 2) {
            int b = buf.readUnsignedByte();
            if (b == 0xfa && buf.isReadable() && buf.getUnsignedByte(buf.readerIndex()) == 0xaf) {
                buf.readUnsignedByte();
                result.writeByte(0xfb);
                result.writeByte(0xbf);
                result.writeByte(0x01);
            } else if (b == 0xfb && buf.isReadable() && buf.getUnsignedByte(buf.readerIndex()) == 0xbf) {
                buf.readUnsignedByte();
                result.writeByte(0xfb);
                result.writeByte(0xbf);
                result.writeByte(0x02);
            } else {
                result.writeByte(b);
            }
        }
        result.writeBytes(buf.readBytes(2));

        return result;
    }

    private static ByteBuf encodeContent(String uniqueId, String content) {
        ByteBuf buf = Unpooled.buffer();

        ByteBuf message = Unpooled.copiedBuffer(content.getBytes());

        buf.writeShort(FLAG);
        buf.writeShort(MSG_COMMAND);
        buf.writeShort(1); // index
        buf.writeBytes(DataConverter.parseHex(uniqueId + "0"));
        buf.writeShort(message.capacity());
        buf.writeShort(Checksum.udp(message.nioBuffer()));
        buf.writeBytes(message);
        buf.writeShort(FLAG);

        return encodeFrame(buf);
    }

    @Override
    protected Object encodeCommand(Command command) {
        String uniqueId = getUniqueId(command.getDeviceId());

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(uniqueId, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC:
                return encodeContent(uniqueId,
                    String.format("tracking_send=%1$d,%1$d", command.getInteger(Command.KEY_FREQUENCY)));
            case Command.TYPE_POWER_OFF:
                return encodeContent(uniqueId, "of=1");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(uniqueId, "reset");
            default:
                return null;
        }
    }

}
