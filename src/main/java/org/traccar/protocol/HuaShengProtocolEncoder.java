/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Command;

public class HuaShengProtocolEncoder extends BaseProtocolEncoder {

    public HuaShengProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(int type, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0xC0);
        buf.writeShort(0x0000); // flag and version
        buf.writeShort(12 + content.readableBytes());
        buf.writeShort(type);
        buf.writeShort(0); // checksum
        buf.writeInt(1); // index
        buf.writeBytes(content);
        content.release();
        buf.writeByte(0xC0);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf content = Unpooled.buffer(0);
        switch (command.getType()) {
            case Command.TYPE_POSITION_PERIODIC:
                content.writeShort(0x0002);
                content.writeShort(6); // length
                content.writeShort(command.getInteger(Command.KEY_FREQUENCY));
                return encodeContent(HuaShengProtocolDecoder.MSG_SET_REQ, content);
            case Command.TYPE_OUTPUT_CONTROL:
                content.writeByte(
                        (command.getInteger(Command.KEY_INDEX) - 1) * 2
                        + (2 - command.getInteger(Command.KEY_DATA)));
                return encodeContent(HuaShengProtocolDecoder.MSG_CTRL_REQ, content);
            case Command.TYPE_ALARM_ARM:
            case Command.TYPE_ALARM_DISARM:
                content.writeShort(0x0001);
                content.writeShort(5); // length
                content.writeByte(command.getType().equals(Command.TYPE_ALARM_ARM) ? 1 : 0);
                return encodeContent(HuaShengProtocolDecoder.MSG_SET_REQ, content);
            case Command.TYPE_SET_SPEED_LIMIT:
                content.writeShort(0x0004);
                content.writeShort(6); // length
                content.writeShort(command.getInteger(Command.KEY_DATA));
                return encodeContent(HuaShengProtocolDecoder.MSG_SET_REQ, content);
            default:
                return null;
        }
    }

}
