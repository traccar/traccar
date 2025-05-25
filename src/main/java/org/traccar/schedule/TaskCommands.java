/*
 * Copyright 2025 Cristiano Nascimento (Cristiano@cpdntech.com.br)
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
package org.traccar.schedule;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import org.traccar.database.CommandsManager;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.QueuedCommand;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TaskCommands extends SingleScheduleTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommands.class);

    private static final long CHECK_PERIOD_MINUTES = 5;

    private final Storage storage;

    private final CommandsManager commandsManager;

    private final LogAction actionLogger;

    @Inject
    public TaskCommands(Storage storage, CommandsManager commandsManager, LogAction actionLogger) {
        this.storage = storage;
        this.commandsManager = commandsManager;
        this.actionLogger = actionLogger;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, CHECK_PERIOD_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        Calendar currentCheck = Calendar.getInstance();
        currentCheck.setTime(new Date());
        currentCheck.set(Calendar.SECOND, 0);
        Calendar lastCheck = Calendar.getInstance();
        lastCheck.setTime(new Date());
        lastCheck.add(java.util.Calendar.MINUTE, (int) -CHECK_PERIOD_MINUTES);
        try {
            List<Command> commands = storage.getObjects(Command.class, new Request(
                    new Columns.All(), new Condition.IsNotNull("calendarid")));

            for (Command command : commands) {
                var calendar = storage.getObject(org.traccar.model.Calendar.class, new Request(
                        new Columns.All(), new Condition.Equals("id", command.getCalendarId())));
                if (calendar == null || calendar.checkMoment(lastCheck.getTime())) {
                    continue; // this command already executed
                }
                if (calendar.checkMomentBetween(lastCheck.getTime(), currentCheck.getTime())) {
                    executeCommand(command);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Scheduled commands error", e);
        }

    }

    private void executeCommand(Command command) throws Exception {
        var deviceIds = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Device.class, Command.class, command.getId())))
                .stream()
                .map(BaseModel::getId)
                .toList();

        var groupIds = storage.getObjects(Group.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Group.class, Command.class, command.getId())))
                .stream().map(BaseModel::getId).toList();

        for (long groupId : groupIds) {
            var devices = storage.getObjects(Device.class, new Request(
                    new Columns.Include("id"),
                    new Condition.Equals("groupId", groupId)));
            for (Device device : devices) {
                try {
                    Command newCmd = QueuedCommand.fromCommand(command).toCommand();
                    newCmd.setDeviceId(device.getId());
                    commandsManager.sendCommand(newCmd);
                } catch (Exception e) {
                    LOGGER.warn("Failed to send command to group {} device {}", groupId, device.getId(), e);
                }
            }
            actionLogger.command(null, 0, groupId, 0, command.getType(), true);
        }
        for (long deviceId : deviceIds) {
            try {
                Command newCmd = QueuedCommand.fromCommand(command).toCommand();
                newCmd.setDeviceId(deviceId);
                commandsManager.sendCommand(newCmd);
                actionLogger.command(null, 0, 0, deviceId, command.getType(), true);
            } catch (Exception e) {
                LOGGER.warn("Failed to send command to device {}", deviceId, e);
            }

        }
    }

}
