/*
 * Copyright 2023 Hossain Mohammad Seym (seym45@gmail.com)
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
import org.traccar.model.Command;

public class GatorProtocolEncoder extends BaseProtocolEncoder {

    public GatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public ByteBuf encodeId(long deviceId) {
        ByteBuf buf = Unpooled.buffer();

        String id = getUniqueId(deviceId);

        int firstDigit = Integer.parseInt(id.substring(1, 3)) - 30;

        buf.writeByte(Integer.parseInt(id.substring(3, 5)) | (((firstDigit >> 3) & 1) << 7));
        buf.writeByte(Integer.parseInt(id.substring(5, 7)) | (((firstDigit >> 2) & 1) << 7));
        buf.writeByte(Integer.parseInt(id.substring(7, 9)) | (((firstDigit >> 1) & 1) << 7));
        buf.writeByte(Integer.parseInt(id.substring(9)) | ((firstDigit & 1) << 7));

        return buf;
    }

    private ByteBuf encodeContent(long deviceId, int type, ByteBuf content) {
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x24);
        buf.writeByte(0x24);
        buf.writeByte(type);
        buf.writeByte(0x00);

        buf.writeByte(4 + 1 + (content != null ? content.readableBytes() : 0) + 1); // length

        ByteBuf pseudoIPAddress = encodeId(deviceId);
        buf.writeBytes(pseudoIPAddress);

        if (content != null) {
            buf.writeBytes(content);
        }

        int checksum = Checksum.xor(buf.nioBuffer());
        buf.writeByte(checksum);

        buf.writeByte(0x0D);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf content = Unpooled.buffer();

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_POSITION_REQUEST, null);
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_CLOSE_OIL_DUCT, null);
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_RESTORE_OIL_DUCT, null);
            case Command.TYPE_SET_SPEED_LIMIT:
                content.writeByte(command.getInteger(Command.KEY_DATA));
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_RESET_MILEAGE, content);
            case Command.TYPE_SET_ODOMETER:
                content.writeShort(command.getInteger(Command.KEY_DATA));
                return encodeContent(command.getDeviceId(), GatorProtocolDecoder.MSG_OVERSPEED_ALARM, content);
            default:
                return null;
        }
    }
}
