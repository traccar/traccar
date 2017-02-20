/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import java.nio.ByteOrder;

public class CellocatorProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(long deviceId, int command, int data1, int data2) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 0);
        buf.writeByte('M');
        buf.writeByte('C');
        buf.writeByte('G');
        buf.writeByte('P');
        buf.writeByte(0);
        buf.writeInt(Integer.parseInt(getUniqueId(deviceId)));
        buf.writeByte(0); // command numerator
        buf.writeInt(0); // authentication code
        buf.writeByte(command);
        buf.writeByte(command);
        buf.writeByte(data1);
        buf.writeByte(data1);
        buf.writeByte(data2);
        buf.writeByte(data2);
        buf.writeInt(0); // command specific data

        byte checksum = 0;
        for (int i = 4; i < buf.writerIndex(); i++) {
            checksum += buf.getByte(i);
        }
        buf.writeByte(checksum);

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_OUTPUT_CONTROL:
                int data = Integer.parseInt(command.getString(Command.KEY_DATA)) << 4
                        + command.getInteger(Command.KEY_INDEX);
                return encodeContent(command.getDeviceId(), 0x03, data, 0);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
