/*
 * Copyright 2017 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.BaseProtocol;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.DataConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class Jt808ProtocolEncoder extends BaseProtocolEncoder {

    public Jt808ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        ByteBuf id = Jt808ProtocolDecoder.encodeId(getUniqueId(command.getDeviceId()));
        try {
            ByteBuf data = Unpooled.buffer();

            switch (command.getType()) {
                case Command.TYPE_CUSTOM:
                    String model = getDeviceModel(command.getDeviceId());
                    if (model != null && Set.of("AL300", "GL100", "VL300").contains(model)) {
                        data.writeByte(1); // number of parameters
                        data.writeInt(0xF030); // AT command transparent transmission
                        int length = command.getString(Command.KEY_DATA).length();
                        data.writeByte(length);
                        data.writeCharSequence(command.getString(Command.KEY_DATA), StandardCharsets.US_ASCII);
                        return Jt808ProtocolDecoder.formatMessage(
                                0x7e, Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                    } else if ("BSJ".equals(model)) {
                        data.writeByte(1); // flag
                        var charset = Charset.isSupported("GBK") ? Charset.forName("GBK") : StandardCharsets.US_ASCII;
                        data.writeCharSequence(command.getString(Command.KEY_DATA), charset);
                        return Jt808ProtocolDecoder.formatMessage(
                                0x7e, Jt808ProtocolDecoder.MSG_SEND_TEXT_MESSAGE, id, false, data);
                    } else {
                        return Unpooled.wrappedBuffer(DataConverter.parseHex(command.getString(Command.KEY_DATA)));
                    }
                case Command.TYPE_REBOOT_DEVICE:
                    data.writeByte(1); // number of parameters
                    data.writeByte(0x23); // parameter id
                    data.writeByte(1); // parameter value length
                    data.writeByte(0x03); // restart
                    return Jt808ProtocolDecoder.formatMessage(
                            0x7e, Jt808ProtocolDecoder.MSG_PARAMETER_SETTING, id, false, data);
                case Command.TYPE_POSITION_PERIODIC:
                    data.writeByte(1); // number of parameters
                    data.writeByte(0x06); // parameter id
                    data.writeByte(4); // parameter value length
                    data.writeInt(command.getInteger(Command.KEY_FREQUENCY));
                    return Jt808ProtocolDecoder.formatMessage(
                            0x7e, Jt808ProtocolDecoder.MSG_PARAMETER_SETTING, id, false, data);
                case Command.TYPE_ALARM_ARM:
                case Command.TYPE_ALARM_DISARM:
                    data.writeByte(1); // number of parameters
                    data.writeByte(0x24); // parameter id
                    String username = "user";
                    data.writeByte(1 + username.length()); // parameter value length
                    data.writeByte(command.getType().equals(Command.TYPE_ALARM_ARM) ? 0x01 : 0x00);
                    data.writeCharSequence(username, StandardCharsets.US_ASCII);
                    return Jt808ProtocolDecoder.formatMessage(
                            0x7e, Jt808ProtocolDecoder.MSG_PARAMETER_SETTING, id, false, data);
                case Command.TYPE_ENGINE_STOP:
                case Command.TYPE_ENGINE_RESUME:
                    if (alternative) {
                        data.writeByte(command.getType().equals(Command.TYPE_ENGINE_STOP) ? 0x01 : 0x00);
                        data.writeBytes(DataConverter.parseHex(
                                new SimpleDateFormat("yyMMddHHmmss").format(new Date())));
                        return Jt808ProtocolDecoder.formatMessage(
                                0x7e, Jt808ProtocolDecoder.MSG_OIL_CONTROL, id, false, data);
                    } else {
                        if ("VL300".equals(getDeviceModel(command.getDeviceId()))) {
                            data.writeCharSequence(command.getType().equals(Command.TYPE_ENGINE_STOP) ? "#0;1" : "#0;0",
                                    StandardCharsets.US_ASCII);
                        } else {
                            data.writeByte(command.getType().equals(Command.TYPE_ENGINE_STOP) ? 0xf0 : 0xf1);
                        }
                        return Jt808ProtocolDecoder.formatMessage(
                                0x7e, Jt808ProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                    }
                case Command.TYPE_VIDEO_START:
                    var config = getCacheManager().getConfig();
                    String host = URI.create(config.getString(Keys.WEB_URL)).getHost();
                    int port = config.getInteger(
                            Keys.PROTOCOL_PORT.withPrefix(BaseProtocol.nameFromClass(Jt1078Protocol.class)));
                    int channel = command.getInteger(Command.KEY_INDEX, 1);
                    data.writeByte(host.length());
                    data.writeCharSequence(host, StandardCharsets.US_ASCII);
                    data.writeShort(port); // tcp port
                    data.writeShort(0); // udp port
                    data.writeByte(channel);
                    data.writeByte(1); // video only
                    data.writeByte(0); // main stream
                    return Jt808ProtocolDecoder.formatMessage(
                            0x7e, Jt808ProtocolDecoder.MSG_VIDEO_REQUEST, id, false, data);
                case Command.TYPE_VIDEO_STOP:
                    data.writeByte(command.getInteger(Command.KEY_INDEX, 1));
                    data.writeByte(0); // close audio/video transmission
                    data.writeByte(0); // close both audio and video
                    data.writeByte(0); // main stream
                    return Jt808ProtocolDecoder.formatMessage(
                            0x7e, Jt808ProtocolDecoder.MSG_VIDEO_CONTROL, id, false, data);
                default:
                    return null;
            }
        } finally {
            id.release();
        }
    }

}
