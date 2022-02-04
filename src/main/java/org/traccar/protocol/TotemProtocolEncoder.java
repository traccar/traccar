/*
 * Copyright 2015 Irving Gonzalez
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;

public class TotemProtocolEncoder extends StringProtocolEncoder {

    public TotemProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    public static String formatContent(Command command) {
        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return String.format("%s,%s",
                                     command.getAttributes().get(Command.KEY_DEVICE_PASSWORD),
                                     command.getAttributes().get(Command.KEY_DATA)
                                    );
            case Command.TYPE_REBOOT_DEVICE:
                return String.format("%s,006", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            case Command.TYPE_FACTORY_RESET:
                return String.format("%s,007", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            case Command.TYPE_GET_VERSION:
                return String.format("%s,056", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            case Command.TYPE_POSITION_SINGLE:
                return String.format("%s,012", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            // Assuming PIN 8 (Output C) is the power wire, like manual says but it can be PIN 5,7,8
            case Command.TYPE_ENGINE_STOP:
                return String.format("%s,025,C,1", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            case Command.TYPE_ENGINE_RESUME:
                return String.format("%s,025,C,0", command.getAttributes().get(Command.KEY_DEVICE_PASSWORD));
            default:
                return null;
        }
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, "000000");

        String commandString = formatContent(command);
        String builtCommand = String.format("$$%04dCF%s", 10 + commandString.getBytes().length, commandString);

        return String.format("%s%02X", builtCommand, Checksum.xor(builtCommand));

    }

}
