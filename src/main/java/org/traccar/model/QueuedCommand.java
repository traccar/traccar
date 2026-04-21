/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.traccar.storage.StorageName;

import java.util.HashMap;

@StorageName("tc_commands_queue")
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueuedCommand extends BaseCommand {

    public static QueuedCommand fromCommand(Command command) {
        QueuedCommand queuedCommand = new QueuedCommand();
        queuedCommand.setDeviceId(command.getDeviceId());
        queuedCommand.setType(command.getType());
        queuedCommand.setTextChannel(command.getTextChannel());
        queuedCommand.setAttributes(new HashMap<>(command.getAttributes()));
        return queuedCommand;
    }

    public Command toCommand() {
        Command command = new Command();
        command.setDeviceId(getDeviceId());
        command.setType(getType());
        command.setDescription("");
        command.setTextChannel(getTextChannel());
        command.setAttributes(new HashMap<>(getAttributes()));
        return command;
    }

}
