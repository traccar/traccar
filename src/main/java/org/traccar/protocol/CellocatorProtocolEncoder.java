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
import org.traccar.model.Command;
import org.traccar.Protocol;

public class CellocatorProtocolEncoder extends BaseProtocolEncoder {

    public CellocatorProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static ByteBuf encodeContent(int type, int uniqueId, int packetNumber, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte('M');
        buf.writeByte('C');
        buf.writeByte('G');
        buf.writeByte('P');
        buf.writeByte(type);
        buf.writeIntLE(uniqueId);
        buf.writeByte(packetNumber);
        buf.writeIntLE(0); // authentication code
        buf.writeBytes(content);

        byte checksum = 0;
        for (int i = 4; i < buf.writerIndex(); i++) {
            checksum += buf.getByte(i);
        }
        buf.writeByte(checksum);

        return buf;
    }

    private ByteBuf encodeCommand(long deviceId, int command, int data1, int data2) {

        ByteBuf content = Unpooled.buffer();
        content.writeByte(command);
        content.writeByte(command);
        content.writeByte(data1);
        content.writeByte(data1);
        content.writeByte(data2);
        content.writeByte(data2);
        content.writeIntLE(0); // command specific data

        ByteBuf buf = encodeContent(0, Integer.parseInt(getUniqueId(deviceId)), 0, content);
        content.release();

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        if (command.getType().equals(Command.TYPE_OUTPUT_CONTROL)) {
            int data = Integer.parseInt(command.getString(Command.KEY_DATA)) << 4
                    + command.getInteger(Command.KEY_INDEX);
            return encodeCommand(command.getDeviceId(), 0x03, data, 0);
        }
        return null;
    }

}
