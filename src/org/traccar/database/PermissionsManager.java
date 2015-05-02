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
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Permission;

public class PermissionsManager {
    
    private final Map<Long, Set<Long>> permissions = new HashMap<Long, Set<Long>>();
    
    private Set<Long> getNotNull(long userId) {
        if (!permissions.containsKey(userId)) {
            permissions.put(userId, new HashSet<Long>());
        }
        return permissions.get(userId);
    }
    
    public PermissionsManager() {
        refresh();
    }
    
    public final void refresh() {
        permissions.clear();
        try {
            for (Permission permission : Context.getDataManager().getPermissions()) {
                getNotNull(permission.getUserId()).add(permission.getDeviceId());
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }
    
    public Collection<Long> allowedDevices(long userId) {
        return getNotNull(userId);
    }
    
    public void checkDevice(long userId, long deviceId) throws SecurityException {
        if (getNotNull(userId).contains(deviceId)) {
            throw new SecurityException();
        }
    }
    
    public void checkDevices(long userId, Collection<Long> devices) throws SecurityException {
        if (getNotNull(userId).containsAll(devices)) {
            throw new SecurityException();
        }
    }
    
}
