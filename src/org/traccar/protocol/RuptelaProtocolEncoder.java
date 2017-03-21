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
import org.traccar.helper.Checksum;
import org.traccar.helper.Log;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class RuptelaProtocolEncoder extends BaseProtocolEncoder {

    private ChannelBuffer encodeContent(int type, ChannelBuffer content) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

        buf.writeShort(1 + content.readableBytes());
        buf.writeByte(100 + type);
        buf.writeBytes(content);
        buf.writeShort(Checksum.crc16(Checksum.CRC16_KERMIT, buf.toByteBuffer(2, buf.writerIndex() - 2)));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ChannelBuffer content = ChannelBuffers.dynamicBuffer();

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                content.writeBytes(command.getString(Command.KEY_DATA).getBytes(StandardCharsets.US_ASCII));
                return encodeContent(RuptelaProtocolDecoder.MSG_SMS_VIA_GPRS, content);
            case Command.TYPE_CONFIGURATION:
                content.writeBytes((command.getString(Command.KEY_DATA) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                return encodeContent(RuptelaProtocolDecoder.MSG_DEVICE_CONFIGURATION, content);
            case Command.TYPE_GET_VERSION:
                return encodeContent(RuptelaProtocolDecoder.MSG_DEVICE_VERSION, content);
            case Command.TYPE_FIRMWARE_UPDATE:
                content.writeBytes("|FU_STRT*\r\n".getBytes(StandardCharsets.US_ASCII));
                return encodeContent(RuptelaProtocolDecoder.MSG_FIRMWARE_UPDATE, content);
            case Command.TYPE_OUTPUT_CONTROL:
                content.writeInt(command.getInteger(Command.KEY_INDEX));
                content.writeInt(Integer.parseInt(command.getString(Command.KEY_DATA)));
                return encodeContent(RuptelaProtocolDecoder.MSG_SET_IO, content);
            case Command.TYPE_SET_CONNECTION:
                String c = command.getString(Command.KEY_SERVER) + "," + command.getInteger(Command.KEY_PORT) + ",TCP";
                content.writeBytes(c.getBytes(StandardCharsets.US_ASCII));
                return encodeContent(RuptelaProtocolDecoder.MSG_SET_CONNECTION, content);
            case Command.TYPE_SET_ODOMETER:
                content.writeInt(Integer.parseInt(command.getString(Command.KEY_DATA)));
                return encodeContent(RuptelaProtocolDecoder.MSG_SET_ODOMETER, content);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
