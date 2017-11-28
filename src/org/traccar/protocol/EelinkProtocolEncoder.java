/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EelinkProtocolEncoder extends BaseProtocolEncoder {

    private boolean connectionless;

    public EelinkProtocolEncoder(boolean connectionless) {
        this.connectionless = connectionless;
    }

    public static int checksum(ByteBuffer buf) {
        int sum = 0;
        while (buf.hasRemaining()) {
            sum = (((sum << 1) | (sum >> 15)) + (buf.get() & 0xFF)) & 0xFFFF;
        }
        return sum;
    }

    private ChannelBuffer encodeContent(long deviceId, String content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        if (connectionless) {
            buf.writeBytes(ChannelBuffers.wrappedBuffer(DatatypeConverter.parseHexBinary('0' + getUniqueId(deviceId))));
        }

        buf.writeShort(EelinkProtocolDecoder.HEADER_KEY);
        buf.writeByte(EelinkProtocolDecoder.MSG_DOWNLINK);
        buf.writeShort(2 + 1 + 4 + content.length()); // data length
        buf.writeShort(0); // index

        buf.writeByte(EelinkProtocolDecoder.MSG_SIGN_COMMAND);
        buf.writeInt(0); // server id
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));

        ChannelBuffer result = ChannelBuffers.dynamicBuffer();

        if (connectionless) {
            result.writeByte('E');
            result.writeByte('L');
            result.writeShort(2 + 2 + 2 + buf.readableBytes()); // length
            result.writeShort(checksum(buf.toByteBuffer()));
        }

        result.writeBytes(buf);

        return result;
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE:
                return encodeContent(command.getDeviceId(), "WHERE#");
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(), "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(), "RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeContent(command.getDeviceId(), "RESET#");
            case Command.TYPE_GET_VERSION:
                return encodeContent(command.getDeviceId(), "VERSION#");
            case Command.TYPE_GET_DEVICE_STATUS:
                return encodeContent(command.getDeviceId(), "STATUS#");
            case Command.TYPE_POSITION_PERIODIC:
                return encodeContent(command.getDeviceId(), "TIMER,"
                        + command.getInteger(Command.KEY_FREQUENCY) + ",1#");
            case Command.TYPE_POSITION_STOP:
                return encodeContent(command.getDeviceId(), "TIMER,0,0#");
            case Command.TYPE_OUTPUT_CONTROL:
                Integer port = command.getInteger(Command.KEY_INDEX);
                Boolean value = command.getBoolean(Command.KEY_DATA);
                if (port < 1 || port > 9) {
                    return null;
                }
                StringBuilder state = new StringBuilder("0000");
                StringBuilder mask = new StringBuilder("0000");
                mask.setCharAt(port - 1, '1');
                state.setCharAt(port - 1, (value ? '1' : '0'));
                return encodeContent(command.getDeviceId(), "PORT," + state + "," + mask + "#");
            case Command.TYPE_SET_TIMEZONE:
                int tz = command.getInteger(Command.KEY_TIMEZONE);
                return encodeContent(command.getDeviceId(), "GMT,"
                        + (tz < 0 ? "E" : "W") + "," + (tz / 3600 / 60) + "#");
            case Command.TYPE_SOS_NUMBER:
                String sosPhoneNumber = command.getString(Command.KEY_DATA);
                if (sosPhoneNumber != null && !sosPhoneNumber.isEmpty()) {
                    return encodeContent(command.getDeviceId(), "SOS,A," + sosPhoneNumber + "#");
                } else {
                    return encodeContent(command.getDeviceId(), "SOS,D#");
                }
            case Command.TYPE_SEND_SMS:
                String phoneNumber = command.getString(Command.KEY_PHONE);
                String message = command.getString(Command.KEY_MESSAGE);
                if (phoneNumber != null && !phoneNumber.isEmpty()
                        && message != null && !message.isEmpty()) {
                    return encodeContent(command.getDeviceId(), "FW," + phoneNumber + "," + message + "#");
                }
                break;
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
