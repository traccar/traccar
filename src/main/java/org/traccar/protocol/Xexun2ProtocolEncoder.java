/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Xexun2ProtocolEncoder extends BaseProtocolEncoder {

    public Xexun2ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

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

    private static int checksum(byte[] data)
    {
        int sum = 0;
        int len = data.length;
        for (int j = 0; len > 1; len--) {
            sum += data[j++] & 0xff;
            if ((sum & 0x80000000) > 0) {
                sum = (sum & 0xffff) + (sum >> 16);
            }
        }
        if (len == 1) {
            sum += data[data.length - 1] & 0xff;
        }
        while ((sum >> 16) > 0) {
            sum = (sum & 0xffff) + sum >> 16;
        }
        sum = (sum == 0xffff) ? sum & 0xffff : (~sum) & 0xffff;
        return sum;
    }


    private static ByteBuf encodeContent(String uniqueId, String content) {
        ByteBuf buf = Unpooled.buffer();

        byte[] message = content.getBytes();

        buf.writeShort(0xFAAF);
        buf.writeShort(0x0007);
        buf.writeShort(0x0001);
        buf.writeBytes(DataConverter.parseHex(uniqueId + "0"),0,8);
        buf.writeShort(message.length);
        buf.writeShort(checksum(message));
        buf.writeBytes(message);
        buf.writeShort(0xFAAF);

        return encodeFrame(buf);
    }

    @Override
    protected Object encodeCommand(Command command) {
        String uniqueId = getUniqueId(command.getDeviceId());

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(uniqueId, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_PERIODIC:
                return encodeContent(uniqueId, String.format("tracking_send=%1$d,%1$d", command.getInteger(Command.KEY_FREQUENCY)));
            case Command.TYPE_POWER_OFF:
                return encodeContent(uniqueId, "of=1");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(uniqueId, "reset");
            default:
                return null;
        }
    }

}
