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

import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WatchProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_TIMEZONE)) {
            double offset = ((Number) value).longValue() / 3600.0;
            DecimalFormat fmt = new DecimalFormat("+#.##;-#.##", DecimalFormatSymbols.getInstance(Locale.US));
            return fmt.format(offset);
        }

        return null;
    }


    protected String formatCommand(Command command, String format, String... keys) {
        String content = super.formatCommand(command, format, this, keys);
        return String.format("[CS*%s*%04x*%s]",
                getUniqueId(command.getDeviceId()), content.length(), content);
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

    private String getBinaryData(Command command) {
        byte[] data = DatatypeConverter.parseHexBinary(command.getString(Command.KEY_DATA));

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

        return new String(encodedData, StandardCharsets.US_ASCII);
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "RG");
            case Command.TYPE_SOS_NUMBER:
                return formatCommand(command, "SOS{%s},{%s}", Command.KEY_INDEX, Command.KEY_PHONE);
            case Command.TYPE_ALARM_SOS:
                return formatCommand(command, "SOSSMS," + getEnableFlag(command));
            case Command.TYPE_ALARM_BATTERY:
                return formatCommand(command, "LOWBAT," + getEnableFlag(command));
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "RESET");
            case Command.TYPE_ALARM_REMOVE:
                return formatCommand(command, "REMOVE," + getEnableFlag(command));
            case Command.TYPE_SILENCE_TIME:
                return formatCommand(command, "SILENCETIME,{%s}", Command.KEY_DATA);
            case Command.TYPE_ALARM_CLOCK:
                return formatCommand(command, "REMIND,{%s}", Command.KEY_DATA);
            case Command.TYPE_SET_PHONEBOOK:
                return formatCommand(command, "PHB,{%s}", Command.KEY_DATA);
            case Command.TYPE_VOICE_MESSAGE:
                return formatCommand(command, "TK," + getBinaryData(command));
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(command, "UPLOAD,{%s}", Command.KEY_FREQUENCY);
            case Command.TYPE_SET_TIMEZONE:
                return formatCommand(command, "LZ,,{%s}", Command.KEY_TIMEZONE);
            case Command.TYPE_SET_INDICATOR:
                return formatCommand(command, "FLOWER,{%s}", Command.KEY_DATA);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
