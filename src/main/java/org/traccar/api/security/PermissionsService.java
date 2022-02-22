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
package org.traccar.api.security;

import org.traccar.model.Calendar;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.GroupedModel;
import org.traccar.model.ScheduledModel;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;

public class PermissionsService {

    private final Storage storage;

    private Server server;
    private User user;

    @Inject
    public PermissionsService(Storage storage) {
        this.storage = storage;
    }

    private Server getServer() throws StorageException {
        if (server == null) {
            server = storage.getObject(
                    Server.class, new Request(new Columns.All()));
        }
        return server;
    }

    private User getUser(long userId) throws StorageException {
        if (user == null) {
            user = storage.getObject(
                    User.class, new Request(new Columns.All(), new Condition.Equals("id", "id", userId)));
        }
        return user;
    }

    public void checkAdmin(long userId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()) {
            throw new SecurityException("Account is readonly");
        }
    }

    public void checkReports(long userId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()
                && (server.getDisableReports() || getUser(userId).getDisableReports())) {
            throw new SecurityException("Reports are disabled");
        }
    }

    public void checkEdit(long userId, Class<?> clazz, boolean addition) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()) {
            boolean denied = false;
            if (getServer().getReadonly() || getUser(userId).getReadonly()) {
                denied = true;
            } else if (clazz.equals(Device.class)) {
                denied = getServer().getDeviceReadonly() || getUser(userId).getDeviceReadonly();
                if (addition) {
                    int deviceCount = storage.getPermissions(User.class, userId, Device.class).size();
                    denied = deviceCount >= getUser(userId).getDeviceLimit();
                }
            } else if (clazz.equals(Command.class)) {
                denied = getServer().getLimitCommands() || getUser(userId).getLimitCommands();
            }
            if (denied) {
                throw new SecurityException("Write access denied");
            }
        }
    }

    public void checkEdit(long userId, Object object, boolean addition) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()) {
            checkEdit(userId, object.getClass(), addition);
            boolean denied = false;
            if (object instanceof GroupedModel) {
                long groupId = ((GroupedModel) object).getGroupId();
                if (groupId > 0) {
                    denied = storage.getPermissions(User.class, userId, Group.class, groupId).isEmpty();
                    // TODO TEST NESTED GROUP PERMISSION
                }
            }
            if (object instanceof ScheduledModel) {
                long calendarId = ((ScheduledModel) object).getCalendarId();
                if (calendarId > 0) {
                    denied = storage.getPermissions(User.class, userId, Calendar.class, calendarId).isEmpty();
                }
            }
            if (denied) {
                throw new SecurityException("Write access denied");
            }
        }
    }

    public void checkUser(long userId, long managedUserId) throws StorageException, SecurityException {
        if (userId != managedUserId && !getUser(userId).getAdministrator()) {
            if (!getUser(userId).getManager()
                    || storage.getPermissions(User.class, userId, User.class, managedUserId).isEmpty()) {
                throw new SecurityException("User access denied");
            }
        }
    }

    public void checkPermission(
            Class<?> clazz, long userId, long objectId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()
                && storage.getPermissions(User.class, userId, clazz, objectId).isEmpty()) {
            // TODO handle nested objects
            throw new SecurityException(clazz.getSimpleName() + " access denied");
        }
    }

}
