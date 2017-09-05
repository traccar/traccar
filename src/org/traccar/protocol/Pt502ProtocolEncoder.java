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

import java.util.TimeZone;

import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

public class Pt502ProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_TIMEZONE)) {
            return String.valueOf(TimeZone.getTimeZone((String) value).getRawOffset() / 3600000);
        }

        return null;
    }

    @Override
    protected String formatCommand(Command command, String format, String... keys) {
        return formatCommand(command, format, this, keys);
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_OUTPUT_CONTROL:
                return formatCommand(command, "#OPC{%s},{%s}\r\n", Command.KEY_INDEX, Command.KEY_DATA);
            case Command.TYPE_SET_TIMEZONE:
                return formatCommand(command, "#TMZ{%s}\r\n", Command.KEY_TIMEZONE);
            case Command.TYPE_ALARM_SPEED:
                return formatCommand(command, "#SPD{%s}\r\n", Command.KEY_DATA);
            case Command.TYPE_REQUEST_PHOTO:
                return formatCommand(command, "#PHO\r\n");
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }

}
