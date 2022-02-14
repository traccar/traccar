/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Attribute;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
import org.traccar.model.ManagedUser;
import org.traccar.model.Notification;
import org.traccar.model.Order;
import org.traccar.model.Permission;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PermissionsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsManager.class);

    private final DataManager dataManager;
    private final UsersManager usersManager;

    private volatile Server server;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Long, Set<Long>> groupPermissions = new HashMap<>();
    private final Map<Long, Set<Long>> devicePermissions = new HashMap<>();
    private final Map<Long, Set<Long>> deviceUsers = new HashMap<>();
    private final Map<Long, Set<Long>> groupDevices = new HashMap<>();

    public PermissionsManager(DataManager dataManager, UsersManager usersManager) {
        this.dataManager = dataManager;
        this.usersManager = usersManager;
        refreshServer();
        refreshDeviceAndGroupPermissions();
    }

    protected final void readLock() {
        lock.readLock().lock();
    }

    protected final void readUnlock() {
        lock.readLock().unlock();
    }

    protected final void writeLock() {
        lock.writeLock().lock();
    }

    protected final void writeUnlock() {
        lock.writeLock().unlock();
    }

    public User getUser(long userId) {
        readLock();
        try {
            return usersManager.getById(userId);
        } finally {
            readUnlock();
        }
    }

    public Set<Long> getGroupPermissions(long userId) {
        readLock();
        try {
            if (!groupPermissions.containsKey(userId)) {
                groupPermissions.put(userId, new HashSet<>());
            }
            return groupPermissions.get(userId);
        } finally {
            readUnlock();
        }
    }

    public Set<Long> getDevicePermissions(long userId) {
        readLock();
        try {
            if (!devicePermissions.containsKey(userId)) {
                devicePermissions.put(userId, new HashSet<>());
            }
            return devicePermissions.get(userId);
        } finally {
            readUnlock();
        }
    }

    private Set<Long> getAllDeviceUsers(long deviceId) {
        readLock();
        try {
            if (!deviceUsers.containsKey(deviceId)) {
                deviceUsers.put(deviceId, new HashSet<>());
            }
            return deviceUsers.get(deviceId);
        } finally {
            readUnlock();
        }
    }

    public Set<Long> getDeviceUsers(long deviceId) {
        Device device = Context.getIdentityManager().getById(deviceId);
        if (device != null && !device.getDisabled()) {
            return getAllDeviceUsers(deviceId);
        } else {
            Set<Long> result = new HashSet<>();
            for (long userId : getAllDeviceUsers(deviceId)) {
                if (getUserAdmin(userId)) {
                    result.add(userId);
                }
            }
            return result;
        }
    }

    public Set<Long> getGroupDevices(long groupId) {
        readLock();
        try {
            if (!groupDevices.containsKey(groupId)) {
                groupDevices.put(groupId, new HashSet<>());
            }
            return groupDevices.get(groupId);
        } finally {
            readUnlock();
        }
    }

    public void refreshServer() {
        try {
            server = dataManager.getServer();
        } catch (StorageException error) {
            LOGGER.warn("Refresh server config error", error);
        }
    }

    public final void refreshDeviceAndGroupPermissions() {
        writeLock();
        try {
            groupPermissions.clear();
            devicePermissions.clear();
            try {
                GroupTree groupTree = new GroupTree(Context.getGroupsManager().getItems(
                        Context.getGroupsManager().getAllItems()),
                        Context.getDeviceManager().getAllDevices());
                for (Permission groupPermission : dataManager.getPermissions(User.class, Group.class)) {
                    Set<Long> userGroupPermissions = getGroupPermissions(groupPermission.getOwnerId());
                    Set<Long> userDevicePermissions = getDevicePermissions(groupPermission.getOwnerId());
                    userGroupPermissions.add(groupPermission.getPropertyId());
                    for (Group group : groupTree.getGroups(groupPermission.getPropertyId())) {
                        userGroupPermissions.add(group.getId());
                    }
                    for (Device device : groupTree.getDevices(groupPermission.getPropertyId())) {
                        userDevicePermissions.add(device.getId());
                    }
                }

                for (Permission devicePermission : dataManager.getPermissions(User.class, Device.class)) {
                    getDevicePermissions(devicePermission.getOwnerId()).add(devicePermission.getPropertyId());
                }

                groupDevices.clear();
                for (long groupId : Context.getGroupsManager().getAllItems()) {
                    for (Device device : groupTree.getDevices(groupId)) {
                        getGroupDevices(groupId).add(device.getId());
                    }
                }

            } catch (StorageException | ClassNotFoundException error) {
                LOGGER.warn("Refresh device permissions error", error);
            }

            deviceUsers.clear();
            for (Map.Entry<Long, Set<Long>> entry : devicePermissions.entrySet()) {
                for (long deviceId : entry.getValue()) {
                    getAllDeviceUsers(deviceId).add(entry.getKey());
                }
            }
        } finally {
            writeUnlock();
        }
    }

    public boolean getUserAdmin(long userId) {
        User user = getUser(userId);
        return user != null && user.getAdministrator();
    }

    public void checkAdmin(long userId) throws SecurityException {
        if (!getUserAdmin(userId)) {
            throw new SecurityException("Admin access required");
        }
    }

    public boolean getUserManager(long userId) {
        User user = getUser(userId);
        return user != null && user.getUserLimit() != 0;
    }

    public void checkManager(long userId) throws SecurityException {
        if (!getUserManager(userId)) {
            throw new SecurityException("Manager access required");
        }
    }

    public void checkManager(long userId, long managedUserId) throws SecurityException {
        checkManager(userId);
        if (!usersManager.getUserItems(userId).contains(managedUserId)) {
            throw new SecurityException("User access denied");
        }
    }

    public void checkUserLimit(long userId) throws SecurityException {
        int userLimit = getUser(userId).getUserLimit();
        if (userLimit != -1 && usersManager.getUserItems(userId).size() >= userLimit) {
            throw new SecurityException("Manager user limit reached");
        }
    }

    public void checkDeviceLimit(long userId) throws SecurityException {
        int deviceLimit = getUser(userId).getDeviceLimit();
        if (deviceLimit != -1) {
            int deviceCount;
            if (getUserManager(userId)) {
                deviceCount = Context.getDeviceManager().getAllManagedItems(userId).size();
            } else {
                deviceCount = Context.getDeviceManager().getAllUserItems(userId).size();
            }
            if (deviceCount >= deviceLimit) {
                throw new SecurityException("User device limit reached");
            }
        }
    }

    public boolean getUserReadonly(long userId) {
        User user = getUser(userId);
        return user != null && user.getReadonly();
    }

    public boolean getUserDeviceReadonly(long userId) {
        User user = getUser(userId);
        return user != null && user.getDeviceReadonly();
    }

    public boolean getUserLimitCommands(long userId) {
        User user = getUser(userId);
        return user != null && user.getLimitCommands();
    }

    public boolean getUserDisableReport(long userId) {
        User user = getUser(userId);
        return user != null && user.getDisableReports();
    }

    public void checkReadonly(long userId) throws SecurityException {
        if (!getUserAdmin(userId) && (server.getReadonly() || getUserReadonly(userId))) {
            throw new SecurityException("Account is readonly");
        }
    }

    public void checkDeviceReadonly(long userId) throws SecurityException {
        if (!getUserAdmin(userId) && (server.getDeviceReadonly() || getUserDeviceReadonly(userId))) {
            throw new SecurityException("Account is device readonly");
        }
    }

    public void checkLimitCommands(long userId) throws SecurityException {
        if (!getUserAdmin(userId) && (server.getLimitCommands() || getUserLimitCommands(userId))) {
            throw new SecurityException("Account has limit sending commands");
        }
    }

    public void checkDisableReports(long userId) throws SecurityException {
        if (!getUserAdmin(userId) && (server.getDisableReports() || getUserDisableReport(userId))) {
            throw new SecurityException("Account has reports disabled");
        }
    }

    public void checkUserDeviceCommand(long userId, long deviceId, long commandId) throws SecurityException {
        if (!getUserAdmin(userId) && Context.getCommandsManager().checkDeviceCommand(deviceId, commandId)) {
            throw new SecurityException("Command can not be sent to this device");
        }
    }

    public void checkUserEnabled(long userId) throws SecurityException {
        User user = getUser(userId);
        if (user == null) {
            throw new SecurityException("Unknown account");
        }
        if (user.getDisabled()) {
            throw new SecurityException("Account is disabled");
        }
        if (user.getExpirationTime() != null && System.currentTimeMillis() > user.getExpirationTime().getTime()) {
            throw new SecurityException("Account has expired");
        }
    }

    public void checkUserUpdate(long userId, User before, User after) throws SecurityException {
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
            }
            if (!getUserAdmin(userId)) {
                checkManager(userId);
            }
        }
    }

    public void checkUser(long userId, long managedUserId) throws SecurityException {
        if (userId != managedUserId && !getUserAdmin(userId)) {
            checkManager(userId, managedUserId);
        }
    }

    public void checkGroup(long userId, long groupId) throws SecurityException {
        if (!getGroupPermissions(userId).contains(groupId) && !getUserAdmin(userId)) {
            checkManager(userId);
            for (long managedUserId : usersManager.getUserItems(userId)) {
                if (getGroupPermissions(managedUserId).contains(groupId)) {
                    return;
                }
            }
            throw new SecurityException("Group access denied");
        }
    }

    public void checkDevice(long userId, long deviceId) throws SecurityException {
        if (!Context.getDeviceManager().getUserItems(userId).contains(deviceId) && !getUserAdmin(userId)) {
            checkManager(userId);
            for (long managedUserId : usersManager.getUserItems(userId)) {
                if (Context.getDeviceManager().getUserItems(managedUserId).contains(deviceId)) {
                    return;
                }
            }
            throw new SecurityException("Device access denied");
        }
    }

    public void checkRegistration(long userId) {
        if (!server.getRegistration() && !getUserAdmin(userId)) {
            throw new SecurityException("Registration disabled");
        }
    }

    public void checkPermission(Class<?> object, long userId, long objectId)
            throws SecurityException {
        SimpleObjectManager<? extends BaseModel> manager = null;

        if (object.equals(Device.class)) {
            checkDevice(userId, objectId);
        } else if (object.equals(Group.class)) {
            checkGroup(userId, objectId);
        } else if (object.equals(User.class) || object.equals(ManagedUser.class)) {
            checkUser(userId, objectId);
        } else if (object.equals(Geofence.class)) {
            manager = Context.getGeofenceManager();
        } else if (object.equals(Attribute.class)) {
            manager = Context.getAttributesManager();
        } else if (object.equals(Driver.class)) {
            manager = Context.getDriversManager();
        } else if (object.equals(Calendar.class)) {
            manager = Context.getCalendarManager();
        } else if (object.equals(Command.class)) {
            manager = Context.getCommandsManager();
        } else if (object.equals(Maintenance.class)) {
            manager = Context.getMaintenancesManager();
        } else if (object.equals(Notification.class)) {
            manager = Context.getNotificationManager();
        } else if (object.equals(Order.class)) {
            manager = Context.getOrderManager();
        } else {
            throw new IllegalArgumentException("Unknown object type");
        }

        if (manager != null && !manager.checkItemPermission(userId, objectId) && !getUserAdmin(userId)) {
            checkManager(userId);
            for (long managedUserId : usersManager.getManagedItems(userId)) {
                if (manager.checkItemPermission(managedUserId, objectId)) {
                    return;
                }
            }
            throw new SecurityException("Type " + object + " access denied");
        }
    }

    public void refreshAllUsersPermissions() {
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refreshUserItems();
        }
        Context.getCalendarManager().refreshUserItems();
        Context.getDriversManager().refreshUserItems();
        Context.getAttributesManager().refreshUserItems();
        Context.getCommandsManager().refreshUserItems();
        Context.getMaintenancesManager().refreshUserItems();
        if (Context.getNotificationManager() != null) {
            Context.getNotificationManager().refreshUserItems();
        }
    }

    public void refreshAllExtendedPermissions() {
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refreshExtendedPermissions();
        }
        Context.getDriversManager().refreshExtendedPermissions();
        Context.getAttributesManager().refreshExtendedPermissions();
        Context.getCommandsManager().refreshExtendedPermissions();
        Context.getMaintenancesManager().refreshExtendedPermissions();
    }

    public void refreshPermissions(Permission permission) {
        if (permission.getOwnerClass().equals(User.class)) {
            if (permission.getPropertyClass().equals(Device.class)
                    || permission.getPropertyClass().equals(Group.class)) {
                refreshDeviceAndGroupPermissions();
                refreshAllExtendedPermissions();
            } else if (permission.getPropertyClass().equals(ManagedUser.class)) {
                usersManager.refreshUserItems();
            } else if (permission.getPropertyClass().equals(Geofence.class) && Context.getGeofenceManager() != null) {
                Context.getGeofenceManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Driver.class)) {
                Context.getDriversManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Attribute.class)) {
                Context.getAttributesManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Calendar.class)) {
                Context.getCalendarManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Command.class)) {
                Context.getCommandsManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Maintenance.class)) {
                Context.getMaintenancesManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Order.class)) {
                Context.getOrderManager().refreshUserItems();
            } else if (permission.getPropertyClass().equals(Notification.class)
                    && Context.getNotificationManager() != null) {
                Context.getNotificationManager().refreshUserItems();
            }
        } else if (permission.getOwnerClass().equals(Device.class) || permission.getOwnerClass().equals(Group.class)) {
            if (permission.getPropertyClass().equals(Geofence.class) && Context.getGeofenceManager() != null) {
                Context.getGeofenceManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Driver.class)) {
                Context.getDriversManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Attribute.class)) {
                Context.getAttributesManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Command.class)) {
                Context.getCommandsManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Maintenance.class)) {
                Context.getMaintenancesManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Order.class)) {
                Context.getOrderManager().refreshExtendedPermissions();
            } else if (permission.getPropertyClass().equals(Notification.class)
                    && Context.getNotificationManager() != null) {
                Context.getNotificationManager().refreshExtendedPermissions();
            }
        }
    }

    public Server getServer() {
        return server;
    }

    public void updateServer(Server server) throws StorageException {
        dataManager.updateObject(server);
        this.server = server;
    }

    public User login(String email, String password) throws StorageException {
        User user = dataManager.login(email, password);
        if (user != null) {
            checkUserEnabled(user.getId());
            return getUser(user.getId());
        }
        return null;
    }

    public Object lookupAttribute(long userId, String key, Object defaultValue) {
        Object preference;
        Object serverPreference = server.getAttributes().get(key);
        Object userPreference = getUser(userId).getAttributes().get(key);
        if (server.getForceSettings()) {
            preference = serverPreference != null ? serverPreference : userPreference;
        } else {
            preference = userPreference != null ? userPreference : serverPreference;
        }
        return preference != null ? preference : defaultValue;
    }

}
