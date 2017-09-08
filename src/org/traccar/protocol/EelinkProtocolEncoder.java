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

import java.nio.charset.StandardCharsets;

public class EelinkProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeShort(EelinkProtocolDecoder.HEADER_KEY);
        buf.writeByte(EelinkProtocolDecoder.MSG_DOWNLINK);
        buf.writeShort(2 + 1 + 4 + content.length()); // data length
        buf.writeShort(0); // index

        buf.writeByte(EelinkProtocolDecoder.MSG_SIGN_COMMAND);
        buf.writeInt(0); // server id
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getString(Command.KEY_DATA));
            case Command.TYPE_ENGINE_STOP:
                return encodeContent("RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent("RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent("RESET#");
            case Command.TYPE_GET_VERSION:
                return encodeContent("VERSION#");
            case Command.TYPE_GET_DEVICE_STATUS:
                return encodeContent("STATUS#");
            case Command.TYPE_POSITION_PERIODIC:
                return encodeContent("TIMER," + command.getInteger(Command.KEY_FREQUENCY) + ",1#");
            case Command.TYPE_POSITION_STOP:
                return encodeContent("TIMER,0,0#");
            case Command.TYPE_SET_TIMEZONE:
                int tz = command.getInteger(Command.KEY_TIMEZONE);
                return encodeContent("GMT," + (tz < 0 ? "E" : "W") + "," + (tz / 3600 / 60) + "#");
            case Command.TYPE_SOS_NUMBER:
                String sosPhoneNumber = command.getString(Command.KEY_DATA);
                if (sosPhoneNumber != null && !sosPhoneNumber.isEmpty()) {
                    return encodeContent("SOS,A," + sosPhoneNumber + "#");
                } else {
                    return encodeContent("SOS,D#");
                }
            case Command.TYPE_SEND_SMS:
                String phoneNumber = command.getString(Command.KEY_PHONE);
                String message = command.getString(Command.KEY_MESSAGE);
                if (phoneNumber != null && !phoneNumber.isEmpty()
                        && message != null && !message.isEmpty()) {
                    return encodeContent("FW," + phoneNumber + "," + message + "#");
                }
                break;
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
