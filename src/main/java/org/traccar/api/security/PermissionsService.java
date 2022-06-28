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

import com.google.inject.servlet.RequestScoped;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.GroupedModel;
import org.traccar.model.ManagedUser;
import org.traccar.model.ScheduledModel;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.model.UserRestrictions;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;

@RequestScoped
public class PermissionsService {

    private final Storage storage;

    private Server server;
    private User user;

    @Inject
    public PermissionsService(Storage storage) {
        this.storage = storage;
    }

    public Server getServer() throws StorageException {
        if (server == null) {
            server = storage.getObject(
                    Server.class, new Request(new Columns.All()));
        }
        return server;
    }

    public User getUser(long userId) throws StorageException {
        if (user == null && userId > 0) {
            if (userId == ServiceAccountUser.ID) {
                user = new ServiceAccountUser();
            } else {
                user = storage.getObject(
                        User.class, new Request(new Columns.All(), new Condition.Equals("id", "id", userId)));
            }
        }
        return user;
    }

    public boolean notAdmin(long userId) throws StorageException {
        return !getUser(userId).getAdministrator();
    }

    public void checkAdmin(long userId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()) {
            throw new SecurityException("Administrator access required");
        }
    }

    public void checkManager(long userId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator() && getUser(userId).getUserLimit() == 0) {
            throw new SecurityException("Manager access required");
        }
    }

    public interface CheckRestrictionCallback {
        boolean denied(UserRestrictions userRestrictions);
    }

    public void checkRestriction(
            long userId, CheckRestrictionCallback callback) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()
                && (callback.denied(getServer()) || callback.denied(getUser(userId)))) {
            throw new SecurityException("Operation restricted");
        }
    }

    public void checkEdit(long userId, Class<?> clazz, boolean addition) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator()) {
            boolean denied = false;
            if (getServer().getReadonly() || getUser(userId).getReadonly()) {
                denied = true;
            } else if (clazz.equals(Device.class)) {
                denied = getServer().getDeviceReadonly() || getUser(userId).getDeviceReadonly()
                        || addition && getUser(userId).getDeviceLimit() == 0;
                if (addition && getUser(userId).getDeviceLimit() > 0) {
                    int deviceCount = storage.getObjects(Device.class, new Request(
                            new Columns.Include("id"),
                            new Condition.Permission(User.class, userId, Device.class))).size();
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
                    checkPermission(Group.class, userId, groupId);
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
                    || storage.getPermissions(User.class, userId, ManagedUser.class, managedUserId).isEmpty()) {
                throw new SecurityException("User access denied");
            }
        }
    }

    public void checkUserUpdate(long userId, User before, User after) throws StorageException, SecurityException {
        if (before.getAdministrator() != after.getAdministrator()
                || before.getDeviceLimit() != after.getDeviceLimit()
                || before.getUserLimit() != after.getUserLimit()) {
            checkAdmin(userId);
        }
        User user = getUser(userId);
        if (user != null && user.getExpirationTime() != null
                && (after.getExpirationTime() == null
                || user.getExpirationTime().compareTo(after.getExpirationTime()) < 0)) {
            checkAdmin(userId);
        }
        if (before.getReadonly() != after.getReadonly()
                || before.getDeviceReadonly() != after.getDeviceReadonly()
                || before.getDisabled() != after.getDisabled()
                || before.getLimitCommands() != after.getLimitCommands()
                || before.getDisableReports() != after.getDisableReports()) {
            if (userId == after.getId()) {
                checkAdmin(userId);
            } else {
                checkUser(userId, after.getId());
            }
        }
    }

    public <T extends BaseModel> void checkPermission(
            Class<T> clazz, long userId, long objectId) throws StorageException, SecurityException {
        if (!getUser(userId).getAdministrator() && !(clazz.equals(User.class) && userId == objectId)) {
            var object = storage.getObject(clazz, new Request(
                    new Columns.Include("id"),
                    new Condition.And(
                            new Condition.Equals("id", "id", objectId),
                            new Condition.Permission(
                                    User.class, userId, clazz.equals(User.class) ? ManagedUser.class : clazz))));
            if (object == null) {
                throw new SecurityException(clazz.getSimpleName() + " access denied");
            }
        }
    }

}
