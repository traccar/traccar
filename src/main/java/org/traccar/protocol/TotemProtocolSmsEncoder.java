/*
 * Copyright 2015 Irving Gonzalez
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

public class TotemProtocolSmsEncoder extends StringProtocolEncoder {

    public TotemProtocolSmsEncoder(Protocol protocol) {
        super(protocol);
    }

    protected String getCommandString(Command command) {
        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCommand(command, "%s,%s", Command.KEY_DEVICE_PASSWORD, Command.KEY_DATA);
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "%s,006", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_FACTORY_RESET:
                return formatCommand(command, "%s,007", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_GET_VERSION:
                return formatCommand(command, "%s,056", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "%s,012", Command.KEY_DEVICE_PASSWORD);
            // Assuming PIN 8 (Output C) is the power wire, like manual says but it can be PIN 5,7,8
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "%s,025,C,1", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "%s,025,C,0", Command.KEY_DEVICE_PASSWORD);
            default:
                return null;
        }
    }

    @Override
    protected String encodeCommand(Command command) {

        initDevicePassword(command, "000000");

        return String.format("*%s#", getCommandString(command));
    }

}
