/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.DevicePermission;
import org.traccar.model.Group;
import org.traccar.model.GroupPermission;
import org.traccar.model.Server;
import org.traccar.model.User;

public class PermissionsManager {

    private final DataManager dataManager;

    private Server server;

    private final Map<Long, User> users = new HashMap<>();

    private final Map<Long, Set<Long>> groupPermissions = new HashMap<>();
    private final Map<Long, Set<Long>> devicePermissions = new HashMap<>();

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

    public PermissionsManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refresh();
    }

    public final void refresh() {
        users.clear();
        groupPermissions.clear();
        devicePermissions.clear();
        try {
            server = dataManager.getServer();
            for (User user : dataManager.getUsers()) {
                users.put(user.getId(), user);
            }

            GroupTree groupTree = new GroupTree(dataManager.getAllGroups());
            for (GroupPermission permission : dataManager.getGroupPermissions()) {
                Set<Long> userGroupPermissions = getGroupPermissions(permission.getUserId());
                userGroupPermissions.add(permission.getGroupId());
                for (Group group : groupTree.getDescendants(permission.getGroupId())) {
                    userGroupPermissions.add(group.getId());
                }
            }

            for (DevicePermission permission : dataManager.getDevicePermissions()) {
                getDevicePermissions(permission.getUserId()).add(permission.getDeviceId());
            }

            for (Device device : dataManager.getAllDevices()) {
                // TODO
            }

        } catch (SQLException error) {
            Log.warning(error);
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

    public void checkReadonly(long userId) {
        if (server.getReadonly() && !isAdmin(userId)) {
            throw new SecurityException("Readonly user");
        }
    }

}
