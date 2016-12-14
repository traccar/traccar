/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Device;
import org.traccar.model.DevicePermission;
import org.traccar.model.Group;
import org.traccar.model.GroupPermission;
import org.traccar.model.Server;
import org.traccar.model.User;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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

    public PermissionsManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshUsers();
        refreshPermissions();
    }

    public final void refreshUsers() {
        users.clear();
        usersTokens.clear();
        try {
            server = dataManager.getServer();
            for (User user : dataManager.getUsers()) {
                users.put(user.getId(), user);
                if (user.getToken() != null) {
                    usersTokens.put(user.getToken(), user.getId());
                }
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
            for (GroupPermission permission : dataManager.getGroupPermissions()) {
                Set<Long> userGroupPermissions = getGroupPermissions(permission.getUserId());
                Set<Long> userDevicePermissions = getDevicePermissions(permission.getUserId());
                userGroupPermissions.add(permission.getGroupId());
                for (Group group : groupTree.getGroups(permission.getGroupId())) {
                    userGroupPermissions.add(group.getId());
                }
                for (Device device : groupTree.getDevices(permission.getGroupId())) {
                    userDevicePermissions.add(device.getId());
                }
            }
            for (DevicePermission permission : dataManager.getDevicePermissions()) {
                getDevicePermissions(permission.getUserId()).add(permission.getDeviceId());
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

    public boolean isReadonly(long userId) {
        return users.containsKey(userId) && users.get(userId).getReadonly();
    }

    public void checkReadonly(long userId) throws SecurityException {
        if (!isAdmin(userId) && (server.getReadonly() || isReadonly(userId))) {
            throw new SecurityException("Account is readonly");
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
                || before.getReadonly() != after.getReadonly()
                || before.getDisabled() != after.getDisabled()
                || before.getDeviceLimit() != after.getDeviceLimit()
                || !Objects.equals(before.getExpirationTime(), after.getExpirationTime())
                || !Objects.equals(before.getToken(), after.getToken())) {
            checkAdmin(userId);
        }
    }

    public void checkUser(long userId, long otherUserId) throws SecurityException {
        if (userId != otherUserId) {
            checkAdmin(userId);
        }
    }

    public void checkGroup(long userId, long groupId) throws SecurityException {
        if (!getGroupPermissions(userId).contains(groupId)) {
            throw new SecurityException("Group access denied");
        }
    }

    public void checkDevice(long userId, long deviceId) throws SecurityException {
        if (!getDevicePermissions(userId).contains(deviceId)) {
            throw new SecurityException("Device access denied");
        }
    }

    public void checkRegistration(long userId) {
        if (!server.getRegistration() && !isAdmin(userId)) {
            throw new SecurityException("Registration disabled");
        }
    }

    public void checkGeofence(long userId, long geofenceId) throws SecurityException {
        if (!Context.getGeofenceManager().checkGeofence(userId, geofenceId) && !isAdmin(userId)) {
            throw new SecurityException("Geofence access denied");
        }
    }

    public void checkCalendar(long userId, long calendarId) throws SecurityException {
        if (!Context.getCalendarManager().checkCalendar(userId, calendarId) && !isAdmin(userId)) {
            throw new SecurityException("Calendar access denied");
        }
    }

    public Server getServer() {
        return server;
    }

    public void updateServer(Server server) throws SQLException {
        dataManager.updateServer(server);
        this.server = server;
    }

    public Collection<User> getUsers() {
        return users.values();
    }

    public User getUser(long userId) {
        return users.get(userId);
    }

    public void addUser(User user) throws SQLException {
        dataManager.addUser(user);
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
        dataManager.removeUser(userId);
        usersTokens.remove(users.get(userId).getToken());
        users.remove(userId);
        refreshPermissions();
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

}
