/*
 * Copyright 2025 Cristiano Nascimento (cristiano@cpdntech.com.br)
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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.CommandsManager;
import org.traccar.helper.LogAction;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.QueuedCommand;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Date;
import java.util.List;

public class TaskCommands extends SingleScheduleTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommands.class);

    private final long checkPeriodMinutes;

    private final Storage storage;

    private final CommandsManager commandsManager;

    private final LogAction actionLogger;

    @Inject
    public TaskCommands(Storage storage, CommandsManager commandsManager, LogAction actionLogger, Config config) {
        this.storage = storage;
        this.commandsManager = commandsManager;
        this.actionLogger = actionLogger;
        this.checkPeriodMinutes = config.getInteger(Keys.TASK_COMMAND_INTERVAL_MINUTES, 15);

    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(this, checkPeriodMinutes, checkPeriodMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        final Date currentCheck = new Date();
        final Date lastCheck = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(checkPeriodMinutes));

        try {
            List<Command> commands = storage.getObjects(Command.class, new Request(new Columns.All(),
                    new Condition.Compare("calendarid", ">", "calendarid", 0)));

            for (Command command : commands) {
                var calendar = storage.getObject(Calendar.class, new Request(
                        new Columns.All(), new Condition.Equals("id", command.getCalendarId())));

                if (calendar == null || calendar.checkMoment(lastCheck)) {
                    continue; // already executed
                }

                if (calendar.checkMoment(currentCheck)) {
                    executeCommand(command);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while executing scheduled commands", e);
        }

    }

    private void executeCommand(final Command command) throws Exception {
        List<Device> devices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Device.class, Command.class, command.getId())));

        List<Long> groupIds = storage.getObjects(Group.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Group.class, Command.class, command.getId())))
                .stream().map(BaseModel::getId).toList();

        for (final long groupId : groupIds) {
            sendCommandToGroup(command, groupId);
        }

        for (final Device device : devices) {
            sendCommandToDevice(command, device);
        }
    }

    private void sendCommandToGroup(final Command command, final long groupId) throws Exception {
        final List<Device> devices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(Group.class, groupId, Device.class)));

        for (final Device device : devices) {
            sendCommandToDevice(command, device);
        }
        actionLogger.command(null, 0, groupId, 0, command.getType(), true);
    }

    private void sendCommandToDevice(final Command command, final Device device) {
        try {
            final Command newCmd = QueuedCommand.fromCommand(command).toCommand();
            newCmd.setDeviceId(device.getId());
            commandsManager.sendCommand(newCmd);
            actionLogger.command(null, 0, 0, device.getId(), command.getType(), true);
            LOGGER.info("Command '{}' sent to device '{}'", command.getType(), device.getUniqueId());
        } catch (Exception e) {
            LOGGER.warn("Failed to send command '{}' to device '{}'", command.getType(), device.getUniqueId(), e);
        }
    }

}
