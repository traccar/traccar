/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class WatchProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public WatchProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_TIMEZONE)) {
            double offset = TimeZone.getTimeZone((String) value).getRawOffset() / 3600000.0;
            DecimalFormat fmt = new DecimalFormat("+#.##;-#.##", DecimalFormatSymbols.getInstance(Locale.US));
            return fmt.format(offset);
        } else if (key.equals(Command.KEY_MESSAGE)) {
            return DataConverter.printHex(value.toString().getBytes(StandardCharsets.UTF_16BE));
        } else if (key.equals(Command.KEY_ENABLE)) {
            return (boolean) value ? "1" : "0";
        }

        return null;
    }

    protected ByteBuf formatTextCommand(Channel channel, Command command, String format, String... keys) {
        String content = formatCommand(command, format, this, keys);
        ByteBuf buf = Unpooled.copiedBuffer(content, StandardCharsets.US_ASCII);

        return formatBinaryCommand(channel, command, "", buf);
    }

    protected ByteBuf formatBinaryCommand(Channel channel, Command command, String textPrefix, ByteBuf data) {
        boolean hasIndex = false;
        String manufacturer = "CS";
        if (channel != null) {
            WatchProtocolDecoder decoder = channel.pipeline().get(WatchProtocolDecoder.class);
            if (decoder != null) {
                hasIndex = decoder.getHasIndex();
                manufacturer = decoder.getManufacturer();
                if (manufacturer.equals("3G")) {
                    manufacturer = "SG";
                }
            }
        }

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte('[');
        buf.writeCharSequence(manufacturer, StandardCharsets.US_ASCII);
        buf.writeByte('*');
        buf.writeCharSequence(getUniqueId(command.getDeviceId()), StandardCharsets.US_ASCII);
        buf.writeByte('*');
        if (hasIndex) {
            buf.writeCharSequence("0001", StandardCharsets.US_ASCII);
            buf.writeByte('*');
        }
        buf.writeCharSequence(String.format("%04x", data.readableBytes() + textPrefix.length()),
                StandardCharsets.US_ASCII);
        buf.writeByte('*');
        buf.writeCharSequence(textPrefix, StandardCharsets.US_ASCII);
        buf.writeBytes(data);
        buf.writeByte(']');

        return buf;
    }

    private static Map<Byte, Byte> mapping = new HashMap<>();

    static {
        mapping.put((byte) 0x7d, (byte) 0x01);
        mapping.put((byte) 0x5B, (byte) 0x02);
        mapping.put((byte) 0x5D, (byte) 0x03);
        mapping.put((byte) 0x2C, (byte) 0x04);
        mapping.put((byte) 0x2A, (byte) 0x05);
    }

    private ByteBuf getBinaryData(Command command) {
        byte[] data = DataConverter.parseHex(command.getString(Command.KEY_DATA));

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

        return Unpooled.copiedBuffer(encodedData);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatTextCommand(channel, command, command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE -> formatTextCommand(channel, command, "CR");
            case Command.TYPE_SOS_NUMBER ->
                    formatTextCommand(channel, command, "SOS%s,%s", Command.KEY_INDEX, Command.KEY_PHONE);
            case Command.TYPE_ALARM_SOS -> formatTextCommand(channel, command, "SOSSMS,%s", Command.KEY_ENABLE);
            case Command.TYPE_ALARM_BATTERY -> formatTextCommand(channel, command, "LOWBAT,%s", Command.KEY_ENABLE);
            case Command.TYPE_REBOOT_DEVICE -> formatTextCommand(channel, command, "RESET");
            case Command.TYPE_POWER_OFF -> formatTextCommand(channel, command, "POWEROFF");
            case Command.TYPE_ALARM_REMOVE -> formatTextCommand(channel, command, "REMOVE,%s", Command.KEY_ENABLE);
            case Command.TYPE_SILENCE_TIME -> formatTextCommand(channel, command, "SILENCETIME,%s", Command.KEY_DATA);
            case Command.TYPE_ALARM_CLOCK -> formatTextCommand(channel, command, "REMIND,%s", Command.KEY_DATA);
            case Command.TYPE_SET_PHONEBOOK -> formatTextCommand(channel, command, "PHB,%s", Command.KEY_DATA);
            case Command.TYPE_MESSAGE -> formatTextCommand(channel, command, "MESSAGE,%s", Command.KEY_MESSAGE);
            case Command.TYPE_VOICE_MESSAGE -> formatBinaryCommand(channel, command, "TK,", getBinaryData(command));
            case Command.TYPE_POSITION_PERIODIC ->
                    formatTextCommand(channel, command, "UPLOAD,%s", Command.KEY_FREQUENCY);
            case Command.TYPE_SET_TIMEZONE ->
                    formatTextCommand(channel, command, "LZ,%s,%s", Command.KEY_LANGUAGE, Command.KEY_TIMEZONE);
            case Command.TYPE_SET_INDICATOR -> formatTextCommand(channel, command, "FLOWER,%s", Command.KEY_DATA);
            default -> null;
        };
    }

}
