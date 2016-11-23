/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class T800xProtocolEncoder extends BaseProtocolEncoder {

    public static final int MODE_SETTING = 0x01;
    public static final int MODE_BROADCAST = 0x02;
    public static final int MODE_FORWARD = 0x03;

    private ChannelBuffer encodeContent(Command command, String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeByte('#');
        buf.writeByte('#');
        buf.writeByte(T800xProtocolDecoder.MSG_COMMAND);
        buf.writeShort(7 + 8 + 1 + content.length());
        buf.writeShort(1); // serial number
        buf.writeBytes(DatatypeConverter.parseHexBinary("0" + getUniqueId(command.getDeviceId())));
        buf.writeByte(MODE_SETTING);
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command, command.getString(Command.KEY_DATA));
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
