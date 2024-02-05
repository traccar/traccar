/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notificators;

import org.traccar.database.CommandsManager;
import org.traccar.model.Command;
import org.traccar.model.Event;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NotificatorCommand implements Notificator {

    private final Storage storage;
    private final CommandsManager commandsManager;

    @Inject
    public NotificatorCommand(Storage storage, CommandsManager commandsManager) {
        this.storage = storage;
        this.commandsManager = commandsManager;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) throws MessageException {

        if (notification == null || notification.getCommandId() <= 0) {
            throw new MessageException("Saved command not provided");
        }

        try {
            Command command = storage.getObject(Command.class, new Request(
                    new Columns.All(), new Condition.Equals("id", notification.getCommandId())));
            command.setDeviceId(event.getDeviceId());
            commandsManager.sendCommand(command);
        } catch (Exception e) {
            throw new MessageException(e);
        }
    }

}
