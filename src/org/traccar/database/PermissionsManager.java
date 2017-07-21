/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Attribute;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.ManagedUser;
import org.traccar.model.Server;
import org.traccar.model.User;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionsManager {

    private final DataManager dataManager;

    private volatile Server server;

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<String, Long> usersTokens = new HashMap<>();

    private final Map<Long, Set<Long>> groupPermissions = new HashMap<>();
    private final Map<Long, Set<Long>> devicePermissions = new HashMap<>();
    private final Map<Long, Set<Long>> deviceUsers = new HashMap<>();
    private final Map<Long, Set<Long>> groupDevices = new HashMap<>();

    private final Map<Long, Set<Long>> userPermissions = new HashMap<>();

    public Set<Long> getGroupPermissions(long userId) {
        if (!groupPermissions.containsKey(userId)) {
            groupPermissions.put(userId, new HashSet<Long>());
        }
        return groupPermissions.get(userId);
    }

    public Set<Long> getDevicePermissions(long userId) {
        if (!devicePermissions.containsKey(userId)) {
            devicePermissions.put(userId, new HashSet<Long>());
        }
        return devicePermissions.get(userId);
    }

    public Set<Long> getDeviceUsers(long deviceId) {
        if (!deviceUsers.containsKey(deviceId)) {
            deviceUsers.put(deviceId, new HashSet<Long>());
        }
        return deviceUsers.get(deviceId);
    }

    public Set<Long> getGroupDevices(long groupId) {
        if (!groupDevices.containsKey(groupId)) {
            groupDevices.put(groupId, new HashSet<Long>());
        }
        return groupDevices.get(groupId);
    }

    public Set<Long> getUserPermissions(long userId) {
        if (!userPermissions.containsKey(userId)) {
            userPermissions.put(userId, new HashSet<Long>());
        }
        return userPermissions.get(userId);
    }

    public PermissionsManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshUsers();
        refreshPermissions();
        refreshUserPermissions();
    }

    public final void refreshUsers() {
        users.clear();
        usersTokens.clear();
        try {
            server = dataManager.getServer();
            for (User user : dataManager.getObjects(User.class)) {
                users.put(user.getId(), user);
                if (user.getToken() != null) {
                    usersTokens.put(user.getToken(), user.getId());
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public final void refreshUserPermissions() {
        userPermissions.clear();
        try {
            for (Map<String, Long> permission : dataManager.getPermissions(User.class, User.class)) {
                getUserPermissions(permission.get(DataManager.makeNameId(User.class)))
                        .add(permission.get(DataManager.makeNameId(ManagedUser.class)));
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public final void refreshPermissions() {
        groupPermissions.clear();
        devicePermissions.clear();
        try {
            GroupTree groupTree = new GroupTree(Context.getDeviceManager().getAllGroups(),
                    Context.getDeviceManager().getAllDevices());
            for (Map<String, Long> groupPermission : dataManager.getPermissions(User.class, Group.class)) {
                Set<Long> userGroupPermissions = getGroupPermissions(groupPermission
                        .get(DataManager.makeNameId(User.class)));
                Set<Long> userDevicePermissions = getDevicePermissions(groupPermission
                        .get(DataManager.makeNameId(User.class)));
                userGroupPermissions.add(groupPermission.get(DataManager.makeNameId(Group.class)));
                for (Group group : groupTree.getGroups(groupPermission.get(DataManager.makeNameId(Group.class)))) {
                    userGroupPermissions.add(group.getId());
                }
                for (Device device : groupTree.getDevices(groupPermission.get(DataManager.makeNameId(Group.class)))) {
                    userDevicePermissions.add(device.getId());
                }
            }

            for (Map<String, Long> devicePermission : dataManager.getPermissions(User.class, Device.class)) {
                getDevicePermissions(devicePermission.get(DataManager.makeNameId(User.class)))
                        .add(devicePermission.get(DataManager.makeNameId(Device.class)));
            }

            groupDevices.clear();
            for (Group group : Context.getDeviceManager().getAllGroups()) {
                for (Device device : groupTree.getDevices(group.getId())) {
                    getGroupDevices(group.getId()).add(device.getId());
                }
            }

        } catch (SQLException error) {
            Log.warning(error);
        }

        deviceUsers.clear();
        for (Map.Entry<Long, Set<Long>> entry : devicePermissions.entrySet()) {
            for (long deviceId : entry.getValue()) {
                getDeviceUsers(deviceId).add(entry.getKey());
            }
        }
    }

    public boolean isAdmin(long userId) {
        return users.containsKey(userId) && users.get(userId).getAdmin();
    }

    public void checkAdmin(long userId) throws SecurityException {
        if (!isAdmin(userId)) {
            throw new SecurityException("Admin access required");
        }
    }

    public boolean isManager(long userId) {
        return users.containsKey(userId) && users.get(userId).getUserLimit() != 0;
    }

    public void checkManager(long userId) throws SecurityException {
        if (!isManager(userId)) {
            throw new SecurityException("Manager access required");
        }
    }

    public void checkManager(long userId, long managedUserId) throws SecurityException {
        checkManager(userId);
        if (!getUserPermissions(userId).contains(managedUserId)) {
            throw new SecurityException("User access denied");
        }
    }

    public void checkUserLimit(long userId) throws SecurityException {
        int userLimit = users.get(userId).getUserLimit();
        if (userLimit != -1 && getUserPermissions(userId).size() >= userLimit) {
            throw new SecurityException("Manager user limit reached");
        }
    }

    public void checkDeviceLimit(long userId) throws SecurityException, SQLException {
        int deviceLimit = users.get(userId).getDeviceLimit();
        if (deviceLimit != -1) {
            int deviceCount = 0;
            if (isManager(userId)) {
                deviceCount = Context.getDeviceManager().getManagedDevices(userId).size();
            } else {
                deviceCount = getDevicePermissions(userId).size();
            }
            if (deviceCount >= deviceLimit) {
                throw new SecurityException("User device limit reached");
            }
        }
    }

    public boolean isReadonly(long userId) {
        return users.containsKey(userId) && users.get(userId).getReadonly();
    }

    public boolean isDeviceReadonly(long userId) {
        return users.containsKey(userId) && users.get(userId).getDeviceReadonly();
    }

    public void checkReadonly(long userId) throws SecurityException {
        if (!isAdmin(userId) && (server.getReadonly() || isReadonly(userId))) {
            throw new SecurityException("Account is readonly");
        }
    }

    public void checkDeviceReadonly(long userId) throws SecurityException {
        if (!isAdmin(userId) && (server.getDeviceReadonly() || isDeviceReadonly(userId))) {
            throw new SecurityException("Account is device readonly");
        }
    }

    public void checkUserEnabled(long userId) throws SecurityException {
        User user = getUser(userId);
        if (user.getDisabled()) {
            throw new SecurityException("Account is disabled");
        }
        if (user.getExpirationTime() != null && System.currentTimeMillis() > user.getExpirationTime().getTime()) {
            throw new SecurityException("Account has expired");
        }
    }

    public void checkUserUpdate(long userId, User before, User after) throws SecurityException {
        if (before.getAdmin() != after.getAdmin()
                || before.getDeviceLimit() != after.getDeviceLimit()
                || before.getUserLimit() != after.getUserLimit()) {
            checkAdmin(userId);
        }
        if (users.containsKey(userId) && users.get(userId).getExpirationTime() != null
                && (after.getExpirationTime() == null
                || users.get(userId).getExpirationTime().compareTo(after.getExpirationTime()) < 0)) {
            checkAdmin(userId);
        }
        if (before.getReadonly() != after.getReadonly()
                || before.getDeviceReadonly() != after.getDeviceReadonly()
                || before.getDisabled() != after.getDisabled()) {
            if (userId == after.getId()) {
                checkAdmin(userId);
            }
            if (!isAdmin(userId)) {
                checkManager(userId);
            }
        }
    }

    public void checkUser(long userId, long managedUserId) throws SecurityException {
        if (userId != managedUserId && !isAdmin(userId)) {
            checkManager(userId, managedUserId);
        }
    }

    public void checkGroup(long userId, long groupId) throws SecurityException {
        if (!getGroupPermissions(userId).contains(groupId) && !isAdmin(userId)) {
            checkManager(userId);
            for (long managedUserId : getUserPermissions(userId)) {
                if (getGroupPermissions(managedUserId).contains(groupId)) {
                    return;
                }
            }
            throw new SecurityException("Group access denied");
        }
    }

    public void checkDevice(long userId, long deviceId) throws SecurityException {
        if (!getDevicePermissions(userId).contains(deviceId) && !isAdmin(userId)) {
            checkManager(userId);
            for (long managedUserId : getUserPermissions(userId)) {
                if (getDevicePermissions(managedUserId).contains(deviceId)) {
                    return;
                }
            }
            throw new SecurityException("Device access denied");
        }
    }

    public void checkRegistration(long userId) {
        if (!server.getRegistration() && !isAdmin(userId)) {
            throw new SecurityException("Registration disabled");
        }
    }

    public void checkPermission(Class<?> object, long userId, long objectId)
            throws SecurityException {
        SimpleObjectManager manager = null;

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
        } else {
            throw new IllegalArgumentException("Unknown object type");
        }

        if (manager != null) {
            if (manager.checkItemPermission(userId, objectId) && !isAdmin(userId)) {
                checkManager(userId);
                for (long managedUserId : getUserPermissions(userId)) {
                    if (manager.checkItemPermission(managedUserId, objectId)) {
                        return;
                    }
                }
                throw new SecurityException("Type " + object + " access denied");
            }
        }
    }

    public void refreshAllExtendedPermissions() {
        if (Context.getGeofenceManager() != null) {
            Context.getGeofenceManager().refreshExtendedPermissions();
        }
        Context.getDriversManager().refreshExtendedPermissions();
        Context.getAttributesManager().refreshExtendedPermissions();
    }

    public void refreshPermissions(Map<String, Long> entity) {
        if (entity.containsKey(DataManager.makeNameId(User.class))) {
            if (entity.containsKey(DataManager.makeNameId(Device.class))
                    || entity.containsKey(DataManager.makeNameId(Group.class))) {
                refreshPermissions();
                refreshAllExtendedPermissions();
            } else if (entity.containsKey(DataManager.makeNameId(ManagedUser.class))) {
                refreshUserPermissions();
            } else if (entity.containsKey(DataManager.makeNameId(Geofence.class))) {
                if (Context.getGeofenceManager() != null) {
                    Context.getGeofenceManager().refreshUserItems();
                }
            } else if (entity.containsKey(DataManager.makeNameId(Driver.class))) {
                Context.getDriversManager().refreshUserItems();
            } else if (entity.containsKey(DataManager.makeNameId(Attribute.class))) {
                Context.getAttributesManager().refreshUserItems();
            } else if (entity.containsKey(DataManager.makeNameId(Calendar.class))) {
                Context.getCalendarManager().refreshUserItems();
            }
        } else if (entity.containsKey(DataManager.makeNameId(Device.class))
                || entity.containsKey(DataManager.makeNameId(Group.class))) {
            if (entity.containsKey(DataManager.makeNameId(Geofence.class))) {
                if (Context.getGeofenceManager() != null) {
                    Context.getGeofenceManager().refreshExtendedPermissions();
                }
            } else if (entity.containsKey(DataManager.makeNameId(Driver.class))) {
                Context.getDriversManager().refreshExtendedPermissions();
            } else if (entity.containsKey(DataManager.makeNameId(Attribute.class))) {
                Context.getAttributesManager().refreshExtendedPermissions();
            }
        }
    }

    public Server getServer() {
        return server;
    }

    public void updateServer(Server server) throws SQLException {
        dataManager.updateObject(server);
        this.server = server;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public Collection<User> getUsers(long userId) {
        Collection<User> result = new ArrayList<>();
        for (long managedUserId : getUserPermissions(userId)) {
            result.add(users.get(managedUserId));
        }
        return result;
    }

    public Collection<User> getManagedUsers(long userId) {
        Collection<User> result = getUsers(userId);
        result.add(users.get(userId));
        return result;
    }

    public User getUser(long userId) {
        return users.get(userId);
    }

    public void addUser(User user) throws SQLException {
        dataManager.addObject(user);
        users.put(user.getId(), user);
        if (user.getToken() != null) {
            usersTokens.put(user.getToken(), user.getId());
        }
        refreshPermissions();
    }

    public void updateUser(User user) throws SQLException {
        dataManager.updateUser(user);
        User old = users.get(user.getId());
        users.put(user.getId(), user);
        if (user.getToken() != null) {
            usersTokens.put(user.getToken(), user.getId());
        }
        if (old.getToken() != null && !old.getToken().equals(user.getToken())) {
            usersTokens.remove(old.getToken());
        }
        refreshPermissions();
    }

    public void removeUser(long userId) throws SQLException {
        dataManager.removeObject(User.class, userId);
        usersTokens.remove(users.get(userId).getToken());
        users.remove(userId);
        refreshPermissions();
        refreshUserPermissions();
    }

    public User login(String email, String password) throws SQLException {
        User user = dataManager.login(email, password);
        if (user != null) {
            checkUserEnabled(user.getId());
            return users.get(user.getId());
        }
        return null;
    }

    public User getUserByToken(String token) {
        return users.get(usersTokens.get(token));
    }

    public Object lookupPreference(long userId, String key, Object defaultValue) {
        String methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
        Object preference;
        Object serverPreference = null;
        Object userPreference = null;
        try {
            Method method = null;
            method = User.class.getMethod(methodName, (Class<?>[]) null);
            if (method != null) {
                userPreference = method.invoke(users.get(userId), (Object[]) null);
            }
            method = null;
            method = Server.class.getMethod(methodName, (Class<?>[]) null);
            if (method != null) {
                serverPreference = method.invoke(server, (Object[]) null);
            }
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException exception) {
            return defaultValue;
        }
        if (server.getForceSettings()) {
            preference = serverPreference != null ? serverPreference : userPreference;
        } else {
            preference = userPreference != null ? userPreference : serverPreference;
        }
        return preference != null ? preference : defaultValue;
    }

}
