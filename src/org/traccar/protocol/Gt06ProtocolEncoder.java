/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.Context;
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.model.Command;
import org.traccar.model.Device;

import java.nio.charset.StandardCharsets;

public class Gt06ProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte(0x78);
        buf.writeByte(0x78);

        buf.writeByte(1 + 1 + 4 + content.length() + 2 + 2); // message length

        buf.writeByte(0x80); // message type

        buf.writeByte(4 + content.length()); // command length
        buf.writeInt(0);
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII)); // command

        buf.writeShort(0); // message index

        buf.writeShort(Checksum.crc16(Checksum.CRC16_X25, buf.toByteBuffer(2, buf.writerIndex() - 2)));

        buf.writeByte('\r');
        buf.writeByte('\n');

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative;
        Device device = Context.getIdentityManager().getDeviceById(command.getDeviceId());
        if (device.getAttributes().containsKey("gt06.alternative")) {
            alternative = device.getBoolean("gt06.alternative");
        } else {
            alternative = Context.getConfig().getBoolean("gt06.alternative");
        }

        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(alternative ? "DYD,123456#" : "Relay,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(alternative ? "HFYD,123456#" : "Relay,0#");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
