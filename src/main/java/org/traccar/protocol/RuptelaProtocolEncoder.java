/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
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
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;

public class RuptelaProtocolEncoder extends BaseProtocolEncoder {

    public RuptelaProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static ByteBuf encodeContent(int type, ByteBuf content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(1 + content.readableBytes());
        buf.writeByte(100 + type);
        buf.writeBytes(content);
        buf.writeShort(Checksum.crc16(Checksum.CRC16_KERMIT, buf.nioBuffer(2, buf.writerIndex() - 2)));

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        ByteBuf content = Unpooled.buffer();

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> {
                String data = command.getString(Command.KEY_DATA);
                if (data.matches("(\\p{XDigit}{2})+")) {
                    content.writeBytes(DataConverter.parseHex(data));
                    yield content;
                } else {
                    content.writeBytes(data.getBytes(StandardCharsets.US_ASCII));
                    yield encodeContent(RuptelaProtocolDecoder.MSG_SMS_VIA_GPRS, content);
                }
            }
            case Command.TYPE_ENGINE_STOP -> {
                content.writeBytes("pass immobilizer 10".getBytes(StandardCharsets.US_ASCII));
                yield encodeContent(RuptelaProtocolDecoder.MSG_SMS_VIA_GPRS, content);
            }
            case Command.TYPE_ENGINE_RESUME -> {
                content.writeBytes("pass resetimmob".getBytes(StandardCharsets.US_ASCII));
                yield encodeContent(RuptelaProtocolDecoder.MSG_SMS_VIA_GPRS, content);
            }
            case Command.TYPE_REQUEST_PHOTO -> {
                content.writeByte(1); // sub-command
                content.writeByte(0); // source
                content.writeInt(0); // start timestamp
                content.writeInt(Integer.MAX_VALUE); // end timestamp
                yield encodeContent(RuptelaProtocolDecoder.MSG_FILES, content);
            }
            case Command.TYPE_CONFIGURATION -> {
                content.writeBytes((command.getString(Command.KEY_DATA) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                yield encodeContent(RuptelaProtocolDecoder.MSG_DEVICE_CONFIGURATION, content);
            }
            case Command.TYPE_GET_VERSION -> encodeContent(RuptelaProtocolDecoder.MSG_DEVICE_VERSION, content);
            case Command.TYPE_FIRMWARE_UPDATE -> {
                content.writeBytes("|FU_STRT*\r\n".getBytes(StandardCharsets.US_ASCII));
                yield encodeContent(RuptelaProtocolDecoder.MSG_FIRMWARE_UPDATE, content);
            }
            case Command.TYPE_OUTPUT_CONTROL -> {
                content.writeInt(command.getInteger(Command.KEY_INDEX));
                content.writeInt(command.getInteger(Command.KEY_DATA));
                yield encodeContent(RuptelaProtocolDecoder.MSG_SET_IO, content);
            }
            case Command.TYPE_SET_CONNECTION -> {
                String c = command.getString(Command.KEY_SERVER) + "," + command.getInteger(Command.KEY_PORT) + ",TCP";
                content.writeBytes(c.getBytes(StandardCharsets.US_ASCII));
                yield encodeContent(RuptelaProtocolDecoder.MSG_SET_CONNECTION, content);
            }
            case Command.TYPE_SET_ODOMETER -> {
                content.writeInt(command.getInteger(Command.KEY_DATA));
                yield encodeContent(RuptelaProtocolDecoder.MSG_SET_ODOMETER, content);
            }
            default -> null;
        };
    }

}
