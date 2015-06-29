/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar;

import org.traccar.database.ActiveDevice;
import org.traccar.command.CommandType;
import org.traccar.command.CommandTemplate;
import org.traccar.command.GpsCommand;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseProtocol implements Protocol {

    private final String name;
    private Map<CommandType, CommandTemplate> commandTemplates = new HashMap<CommandType, CommandTemplate>();

    public BaseProtocol(String name) {
        this.name = name;
        this.initCommandsTemplates(commandTemplates);
    }

    public String getName() {
        return name;
    }

    @Override
    public void sendCommand(ActiveDevice activeDevice, GpsCommand command) {
        CommandTemplate commandMessage = commandTemplates.get(command.getType());

        if (commandMessage == null) {
            throw new RuntimeException("The command " + command + " is not yet supported in protocol " + this.getName());
        }

        Object response = commandMessage.applyTo(activeDevice, command);

        activeDevice.write(response);
    }

    protected void initCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
    }

}
