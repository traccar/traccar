/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

public class JmakProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public JmakProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_ENABLE)) {
            return (Boolean) value ? "1" : "0";
        }
        return null;
    }

    public static int getOutputParameter(Command command) {
        return switch (command.getInteger(Command.KEY_INDEX)) {
            case 2 -> 94;
            case 3 -> 167;
            default -> 34;
        };
    }

    public static int getServerParameter(Command command) {
        return command.getString(Command.KEY_SERVER).matches("[\\d.]+") ? 13 : 43;
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, "%s", Command.KEY_DATA);
            case Command.TYPE_ENGINE_STOP -> "$34;1;#";
            case Command.TYPE_ENGINE_RESUME -> "$34;0;#";
            case Command.TYPE_OUTPUT_CONTROL -> formatCommand(
                    command, "$" + getOutputParameter(command) + ";%s;#", Command.KEY_DATA);
            case Command.TYPE_POSITION_PERIODIC -> formatCommand(command, "$2;%s;#", Command.KEY_FREQUENCY);
            case Command.TYPE_SET_ODOMETER -> formatCommand(command, "$30;%s;#", Command.KEY_DATA);
            case Command.TYPE_SET_CONNECTION -> formatCommand(
                    command, "$" + getServerParameter(command) + ";%s;|12;%s;#", Command.KEY_SERVER, Command.KEY_PORT);
            case Command.TYPE_REBOOT_DEVICE -> "$93;1;#";
            case Command.TYPE_SET_SPEED_LIMIT -> formatCommand(command, "$16;1;|17;%s;#", Command.KEY_DATA);
            case Command.TYPE_ALARM_BATTERY -> formatCommand(command, "$57;%s;#", this, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_REMOVE -> formatCommand(command, "$55;%s;#", this, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_VIBRATION -> formatCommand(command, "$59;%s;#", Command.KEY_DATA);
            default -> null;
        };
    }

}
