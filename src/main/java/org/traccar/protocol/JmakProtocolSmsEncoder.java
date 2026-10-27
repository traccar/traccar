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

public class JmakProtocolSmsEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public JmakProtocolSmsEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {
        if (key.equals(Command.KEY_ENABLE)) {
            return (Boolean) value ? "1" : "0";
        }
        return null;
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, "%s", Command.KEY_DATA);
            case Command.TYPE_ENGINE_STOP -> "JMAK#s;34;1#";
            case Command.TYPE_ENGINE_RESUME -> "JMAK#s;34;0#";
            case Command.TYPE_OUTPUT_CONTROL -> formatCommand(
                    command, "JMAK#s;" + JmakProtocolEncoder.getOutputParameter(command) + ";%s#", Command.KEY_DATA);
            case Command.TYPE_POSITION_PERIODIC -> formatCommand(command, "JMAK#s;2;%s#", Command.KEY_FREQUENCY);
            case Command.TYPE_SET_ODOMETER -> formatCommand(command, "JMAK#s;30;%s#", Command.KEY_DATA);
            case Command.TYPE_SET_CONNECTION -> formatCommand(
                    command, "JMAK#s;" + JmakProtocolEncoder.getServerParameter(command) + ";%s#s;12;%s#",
                    Command.KEY_SERVER, Command.KEY_PORT);
            case Command.TYPE_REBOOT_DEVICE -> "JMAK#REBOOT#";
            case Command.TYPE_SET_SPEED_LIMIT -> formatCommand(command, "JMAK#e;16;1#s;17;%s#", Command.KEY_DATA);
            case Command.TYPE_ALARM_BATTERY -> formatCommand(command, "JMAK#e;57;%s#", this, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_REMOVE -> formatCommand(command, "JMAK#e;55;%s#", this, Command.KEY_ENABLE);
            case Command.TYPE_ALARM_VIBRATION -> formatCommand(command, "JMAK#e;59;%s#", Command.KEY_DATA);
            default -> null;
        };
    }

}
