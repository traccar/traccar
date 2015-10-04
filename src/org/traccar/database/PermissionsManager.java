/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.model.Permission;
import org.traccar.model.User;

public class PermissionsManager {

    private final DataManager dataManager;

    private final Map<Long, User> users = new HashMap<>();

    private final Map<Long, Set<Long>> permissions = new HashMap<>();

    private Set<Long> getNotNull(long userId) {
        if (!permissions.containsKey(userId)) {
            permissions.put(userId, new HashSet<Long>());
        }
        return permissions.get(userId);
    }

    public PermissionsManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refresh();
    }

    public final void refresh() {
        users.clear();
        permissions.clear();
        try {
            for (User user : dataManager.getUsers()) {
                users.put(user.getId(), user);
            }
            for (Permission permission : dataManager.getPermissions()) {
                getNotNull(permission.getUserId()).add(permission.getDeviceId());
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public void checkAdmin(long userId) throws SecurityException {
        if (!users.containsKey(userId) || !users.get(userId).getAdmin()) {
            throw new SecurityException("Admin access required");
        }
    }

    public void checkUser(long userId, long otherUserId) throws SecurityException {
        if (userId != otherUserId) {
            checkAdmin(userId);
        }
    }

    public Collection<Long> allowedDevices(long userId) {
        return getNotNull(userId);
    }

    public void checkDevice(long userId, long deviceId) throws SecurityException {
        if (!getNotNull(userId).contains(deviceId)) {
            throw new SecurityException("Device access denied");
        }
    }

}
