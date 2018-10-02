/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.DataConverter;
import org.traccar.helper.Log;
import org.traccar.model.Command;
import org.traccar.NetworkMessage;
import org.traccar.Context;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;

public class WatchProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    private final boolean enableFramelogging;
    private String lastencodedCmd = new String();

    public WatchProtocolEncoder(WatchProtocol protocol) {
        enableFramelogging = Context.getConfig().getBoolean(protocol.getName() + ".enableFramelogging");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (ctx.handler() instanceof WatchProtocolEncoder && enableFramelogging) {
            logFrame(msg);
        }
    }

    private void logFrame(Object msg) {
        StringBuilder logmsg = new StringBuilder();
        NetworkMessage networkMessage = (NetworkMessage) msg;
        logmsg.append("transmitted frame: ");
        if (networkMessage.getMessage() instanceof Command) {
            logmsg.append(lastencodedCmd);
        } else {
            logmsg.append(networkMessage.getMessage().toString());
        }
        Log.debug(logmsg.toString());
    }

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_TIMEZONE)) {
            double offset = TimeZone.getTimeZone((String) value).getRawOffset() / 3600000.0;
            DecimalFormat fmt = new DecimalFormat("+#.##;-#.##", DecimalFormatSymbols.getInstance(Locale.US));
            return fmt.format(offset);
        }
        if (key.equals(Command.KEY_BLOB)) {
            return getBinaryData((String) value);
        }
        return null;
    }

    protected String formatCommand(Channel channel, Command command, String format, String... keys) {

        boolean hasIndex = false;
        String manufacturer = "CS";
        if (channel != null) {
            WatchProtocolDecoder decoder = channel.pipeline().get(WatchProtocolDecoder.class);
            if (decoder != null) {
                hasIndex = decoder.getHasIndex();
                manufacturer = decoder.getManufacturer();
            }
        }

        String content = formatCommand(command, format, this, keys);

        if (hasIndex) {
            return String.format("[%s*%s*0001*%04x*%s]",
                    manufacturer, getUniqueId(command.getDeviceId()), content.length(), content);
        } else {
            return String.format("[%s*%s*%04x*%s]",
                    manufacturer, getUniqueId(command.getDeviceId()), content.length(), content);
        }
    }

    private int getEnableFlag(Command command) {
        if (command.getBoolean(Command.KEY_ENABLE)) {
            return 1;
        } else {
            return 0;
        }
    }

    private static Map<Byte, Byte> mapping = new HashMap<>();

    static {
        mapping.put((byte) 0x7d, (byte) 0x01);
        mapping.put((byte) 0x5B, (byte) 0x02);
        mapping.put((byte) 0x5D, (byte) 0x03);
        mapping.put((byte) 0x2C, (byte) 0x04);
        mapping.put((byte) 0x2A, (byte) 0x05);
    }

    private String getBinaryData(String content) {
        byte[] data = DataConverter.parseHex(content);

        int encodedLength = data.length;
        for (byte b : data) {
            if (mapping.containsKey(b)) {
                encodedLength += 1;
            }
        }

        int index = 0;
        byte[] encodedData = new byte[encodedLength];

        for (byte b : data) {
            Byte replacement = mapping.get(b);
            if (replacement != null) {
                encodedData[index] = 0x7D;
                index += 1;
                encodedData[index] = replacement;
            } else {
                encodedData[index] = b;
            }
            index += 1;
        }

        return new String(encodedData, StandardCharsets.ISO_8859_1);
    }

    private String extractAudiofileReference(String keyData) {
        String filePath;
        if (keyData.length() > 7 && keyData.substring(0, 7).equals("file://")) {
            filePath = keyData.substring(7, keyData.length());
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            return (filePath);
        }
        return null;
    }

    private byte[] getAudiofileBlob(String filePath) {
        return (Context.getMediaManager().readFile(filePath));
    }

    private void prepareVoiceMessageData(Command command) {
        String blob;
        String filePath = extractAudiofileReference(command.getString(Command.KEY_DATA));
        if (filePath != null) {
            blob = DataConverter.printHex(getAudiofileBlob(filePath));
        } else {
            blob = command.getString(Command.KEY_DATA);
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        command.setAttributes(attributes);
        command.set(Command.KEY_BLOB, blob);
    }

    private String logVoiceMessageFrame(Object encodedCmd) {
        if (!enableFramelogging) {
            return null;
        }
        String logencodedCmd;
        logencodedCmd = Matcher.quoteReplacement((String) encodedCmd);
        logencodedCmd = logencodedCmd.substring(0, Math.min(logencodedCmd.length(), 60));
        logencodedCmd += "...";
        return logencodedCmd;
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {
        Object cmdtoSend;

        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                cmdtoSend = formatCommand(channel, command, command.getString(Command.KEY_DATA));
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_POSITION_SINGLE:
                cmdtoSend = formatCommand(channel, command, "RG");
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_SOS_NUMBER:
                cmdtoSend = formatCommand(channel, command, "SOS{%s},{%s}", Command.KEY_INDEX, Command.KEY_PHONE);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_ALARM_SOS:
                cmdtoSend = formatCommand(channel, command, "SOSSMS," + getEnableFlag(command));
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_ALARM_BATTERY:
                cmdtoSend = formatCommand(channel, command, "LOWBAT," + getEnableFlag(command));
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_REBOOT_DEVICE:
                cmdtoSend = formatCommand(channel, command, "RESET");
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_ALARM_REMOVE:
                cmdtoSend = formatCommand(channel, command, "REMOVE," + getEnableFlag(command));
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_SILENCE_TIME:
                cmdtoSend = formatCommand(channel, command, "SILENCETIME,{%s}", Command.KEY_DATA);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_ALARM_CLOCK:
                cmdtoSend = formatCommand(channel, command, "REMIND,{%s}", Command.KEY_DATA);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_SET_PHONEBOOK:
                cmdtoSend = formatCommand(channel, command, "PHB,{%s}", Command.KEY_DATA);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_VOICE_MESSAGE:
                prepareVoiceMessageData(command);
                cmdtoSend = formatCommand(channel, command, "TK,{%s}", Command.KEY_BLOB);
                lastencodedCmd = logVoiceMessageFrame(cmdtoSend);
                break;
            case Command.TYPE_POSITION_PERIODIC:
                cmdtoSend = formatCommand(channel, command, "UPLOAD,{%s}", Command.KEY_FREQUENCY);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_SET_TIMEZONE:
                cmdtoSend = formatCommand(channel, command, "LZ,,{%s}", Command.KEY_TIMEZONE);
                lastencodedCmd = (String) cmdtoSend;
                break;
            case Command.TYPE_SET_INDICATOR:
                cmdtoSend = formatCommand(channel, command, "FLOWER,{%s}", Command.KEY_DATA);
                lastencodedCmd = (String) cmdtoSend;
                break;
            default:
                cmdtoSend = null;
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }
        ByteBuf commandBuf = Unpooled.buffer();
        commandBuf.writeBytes(((String) cmdtoSend).getBytes(StandardCharsets.ISO_8859_1));
        return (commandBuf);
    }

}
