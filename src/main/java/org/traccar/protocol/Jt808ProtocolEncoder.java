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
import io.netty.channel.Channel;
import org.traccar.BasePipelineFactory;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Jt808ProtocolEncoder extends BaseProtocolEncoder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyMMddHHmmss").withZone(ZoneId.systemDefault());

    public Jt808ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        Jt808ProtocolDecoder decoder = BasePipelineFactory.getHandler(
                channel.pipeline(), Jt808ProtocolDecoder.class);

        boolean alternative = AttributeUtil.lookup(
                getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        Integer protocolVersion = decoder.getProtocolVersion();
        ByteBuf id = Jt808ProtocolDecoder.encodeId(
                getUniqueId(command.getDeviceId()), protocolVersion != null ? 10 : 6);
        String model = getDeviceModel(command.getDeviceId());
        try {
            ByteBuf data = Unpooled.buffer();
            var charset = Charset.isSupported("GBK") ? Charset.forName("GBK") : StandardCharsets.US_ASCII;

            // References:
            // - JT/T 808 standard: 道路运输车辆卫星定位系统 终端通信协议及数据格式 (2013 & 2019 editions)
            // - EMQX JT/T 808 gateway data exchange format (parameter value types):
            //   https://docs.emqx.com/en/emqx/latest/gateway/jt808_data_exchange.html
            switch (command.getType()) {
                case Command.TYPE_CUSTOM:
                    if (model != null && Set.of("AL300", "GL100", "VL300").contains(model)) {
                        data.writeByte(1); // number of parameters
                        data.writeInt(0xF030); // AT command transparent transmission
                        int length = command.getString(Command.KEY_DATA).length();
                        data.writeByte(length);
                        data.writeCharSequence(command.getString(Command.KEY_DATA), StandardCharsets.US_ASCII);
                        return decoder.formatMessage(
                                Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                    } else if (model != null && Set.of("BSJ", "C5", "C5L").contains(model)) {
                        data.writeByte(1); // flag
                        data.writeCharSequence(command.getString(Command.KEY_DATA), charset);
                        return decoder.formatMessage(
                                Jt808ProtocolDecoder.MSG_SEND_TEXT_MESSAGE, id, false, data);
                    } else if (model != null && model.startsWith("JC")) {
                        data.writeByte(0xF0); // online command
                        data.writeCharSequence(command.getString(Command.KEY_DATA), StandardCharsets.US_ASCII);
                        return decoder.formatMessage(
                                Jt808ProtocolDecoder.MSG_TRANSPARENT_DOWNLINK, id, false, data);
                    } else {
                        return Unpooled.wrappedBuffer(DataConverter.parseHex(command.getString(Command.KEY_DATA)));
                    }
                case Command.TYPE_REBOOT_DEVICE:
                    data.writeByte(0x04); // terminal control: reboot
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                case Command.TYPE_POSITION_PERIODIC:
                    data.writeByte(1); // number of parameters
                    data.writeInt(0x0029); // parameter id: default report interval
                    data.writeByte(4); // parameter value length
                    data.writeInt(command.getInteger(Command.KEY_FREQUENCY));
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                case Command.TYPE_POSITION_SINGLE:
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_LOCATION_QUERY, id, false, data);
                case Command.TYPE_POSITION_STOP:
                    data.writeShort(0); // time interval
                    data.writeInt(0); // validity period (dword)
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_TEMPORARY_TRACKING, id, false, data);
                case Command.TYPE_ALARM_ARM:
                case Command.TYPE_ALARM_DISARM:
                    data.writeByte(1); // number of parameters
                    data.writeInt(0x0024); // parameter id: arm/disarm
                    String username = "user";
                    data.writeByte(1 + username.length()); // parameter value length
                    data.writeByte(command.getType().equals(Command.TYPE_ALARM_ARM) ? 0x01 : 0x00);
                    data.writeCharSequence(username, StandardCharsets.US_ASCII);
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                case Command.TYPE_ALARM_DISMISS:
                    data.writeShort(0); // response serial number
                    data.writeInt(0); // alarm type (dword, 0 = confirm all normal alarms)
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_ALARM_ACK, id, false, data);
                case Command.TYPE_ENGINE_STOP:
                case Command.TYPE_ENGINE_RESUME:
                    if (alternative) {
                        data.writeByte(command.getType().equals(Command.TYPE_ENGINE_STOP) ? 0x01 : 0x00);
                        data.writeBytes(DataConverter.parseHex(DATE_FORMAT.format(Instant.now())));
                        return decoder.formatMessage(
                                Jt808ProtocolDecoder.MSG_OIL_CONTROL, id, false, data);
                    } else {
                        if ("VL300".equals(model)) {
                            data.writeCharSequence(command.getType().equals(Command.TYPE_ENGINE_STOP) ? "#0;1" : "#0;0",
                                    StandardCharsets.US_ASCII);
                        } else if ("W15L".equals(model)) {
                            data.writeByte(command.getType().equals(Command.TYPE_ENGINE_STOP) ? 0x64 : 0x65);
                        } else {
                            data.writeByte(command.getType().equals(Command.TYPE_ENGINE_STOP) ? 0xf0 : 0xf1);
                        }
                        return decoder.formatMessage(
                                Jt808ProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                    }
                case Command.TYPE_SET_CONNECTION:
                    data.writeByte(2); // number of parameters
                    String server = command.getString(Command.KEY_SERVER);
                    // Server address (0x13) and TCP port (0x18) use the same standard parameter
                    // IDs in both JT/T 808-2013 and 2019; 0x0001 is heartbeat interval and
                    // 0x0002 is TCP response timeout, so no version-specific branch is needed.
                    data.writeInt(0x13); // parameter id: server address
                    data.writeByte(server.length()); // parameter value length
                    data.writeCharSequence(server, StandardCharsets.US_ASCII);
                    data.writeInt(0x18); // parameter id: server tcp port
                    data.writeByte(4); // parameter value length
                    data.writeInt(command.getInteger(Command.KEY_PORT));
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                case Command.TYPE_POWER_OFF:
                    data.writeByte(0x02); // terminal control: power off
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                case Command.TYPE_FACTORY_RESET:
                    data.writeByte(0x01); // terminal control: restore factory settings
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_TERMINAL_CONTROL, id, false, data);
                case Command.TYPE_MESSAGE:
                    data.writeByte(0x04); // text flag: display on terminal screen
                    data.writeCharSequence(command.getString(Command.KEY_MESSAGE), charset);
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_SEND_TEXT_MESSAGE, id, false, data);
                case Command.TYPE_VOICE_MESSAGE:
                    data.writeByte(0x08); // text flag: TTS voice broadcast
                    data.writeCharSequence(command.getString(Command.KEY_MESSAGE), charset);
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_SEND_TEXT_MESSAGE, id, false, data);
                case Command.TYPE_REQUEST_PHOTO:
                    data.writeByte(command.getInteger(Command.KEY_INDEX, 0)); // channel id
                    data.writeShort(1); // shooting command: take one photo
                    data.writeShort(0); // photo interval / video duration
                    data.writeByte(0); // save flag: real-time upload
                    data.writeByte(0); // resolution
                    data.writeByte(0); // image quality
                    data.writeByte(0); // brightness
                    data.writeByte(0); // contrast
                    data.writeByte(0); // saturation
                    data.writeByte(0); // chroma
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_TAKE_PHOTO, id, false, data);
                case Command.TYPE_SET_SPEED_LIMIT:
                    data.writeByte(2); // number of parameters
                    data.writeInt(0x0055); // parameter id: overspeed alarm speed threshold
                    data.writeByte(4); // parameter value length
                    data.writeInt(command.getInteger(Command.KEY_DATA));
                    data.writeInt(0x0056); // parameter id: overspeed alarm duration
                    data.writeByte(4); // parameter value length
                    data.writeInt(35); // default duration in seconds (no dedicated traccar command field)
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_CONFIGURATION_PARAMETERS, id, false, data);
                case Command.TYPE_OUTPUT_CONTROL:
                    data.writeByte(command.getInteger(Command.KEY_INDEX, 0)); // control flag (2013: single byte)
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_VEHICLE_CONTROL, id, false, data);
                case Command.TYPE_CONFIGURATION:
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_PARAMETER_QUERY_ALL, id, false, data);
                case Command.TYPE_VIDEO_START:
                    var config = getCacheManager().getConfig();
                    String host = URI.create(config.getString(Keys.WEB_URL)).getHost();
                    int port = config.getInteger(
                            Keys.PROTOCOL_PORT.withPrefix(BaseProtocol.nameFromClass(Jt1078Protocol.class)));
                    int videoChannel = command.getInteger(Command.KEY_INDEX, 1);
                    data.writeByte(host.length());
                    data.writeCharSequence(host, StandardCharsets.US_ASCII);
                    data.writeShort(port); // tcp port
                    data.writeShort(0); // udp port
                    data.writeByte(videoChannel);
                    data.writeByte(1); // video only
                    data.writeByte(0); // main stream
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_VIDEO_REQUEST, id, false, data);
                case Command.TYPE_VIDEO_STOP:
                    data.writeByte(command.getInteger(Command.KEY_INDEX, 1));
                    data.writeByte(0); // close audio/video transmission
                    data.writeByte(0); // close both audio and video
                    data.writeByte(0); // main stream
                    return decoder.formatMessage(
                            Jt808ProtocolDecoder.MSG_VIDEO_CONTROL, id, false, data);
                default:
                    return null;
            }
        } finally {
            id.release();
        }
    }

}
