/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.BaseDevicePermission;
import org.traccar.model.BaseGroupPermission;
import org.traccar.model.BaseUserPermission;
import org.traccar.model.Device;
import org.traccar.model.BaseModel;

public abstract class ExtendedObjectManager extends SimpleObjectManager {

    private final Map<Long, Set<Long>> deviceItems = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceItemsWithGroups = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> groupItems = new ConcurrentHashMap<>();

    private Class<? extends BaseDevicePermission> devicePermissionClass;
    private Class<? extends BaseGroupPermission> groupPermissionClass;

    protected ExtendedObjectManager(DataManager dataManager,
            Class<? extends BaseModel> baseClass,
            Class<? extends BaseUserPermission> permissionClass,
            Class<? extends BaseDevicePermission> devicePermissionClass,
            Class<? extends BaseGroupPermission> groupPermissionClass) {
        super(dataManager, baseClass, permissionClass);
        this.devicePermissionClass = devicePermissionClass;
        this.groupPermissionClass = groupPermissionClass;
    }

    public final Set<Long> getGroupItems(long groupId) {
        if (!groupItems.containsKey(groupId)) {
            groupItems.put(groupId, new HashSet<Long>());
        }
        return groupItems.get(groupId);
    }

    protected final void clearGroupItems() {
        groupItems.clear();
    }

    public final Set<Long> getDeviceItems(long deviceId) {
        if (!deviceItems.containsKey(deviceId)) {
            deviceItems.put(deviceId, new HashSet<Long>());
        }
        return deviceItems.get(deviceId);
    }

    protected final void clearDeviceItems() {
        deviceItems.clear();
    }

    public Set<Long> getAllDeviceItems(long deviceId) {
        if (!deviceItemsWithGroups.containsKey(deviceId)) {
            deviceItemsWithGroups.put(deviceId, new HashSet<Long>());
        }
        return deviceItemsWithGroups.get(deviceId);
    }

    @Override
    public void removeItem(long itemId) throws SQLException {
        super.removeItem(itemId);
        refreshExtendedPermissions();
    }

    public void refreshExtendedPermissions() {
        if (getDataManager() != null) {
            try {

                Collection<? extends BaseGroupPermission> databaseGroupPermissions =
                        getDataManager().getObjects(groupPermissionClass);

                clearGroupItems();
                for (BaseGroupPermission groupPermission : databaseGroupPermissions) {
                    getGroupItems(groupPermission.getGroupId()).add(groupPermission.getSlaveId());
                }

                Collection<? extends BaseDevicePermission> databaseDevicePermissions =
                        getDataManager().getObjects(devicePermissionClass);
                Collection<Device> allDevices = Context.getDeviceManager().getAllDevices();

                clearDeviceItems();
                deviceItemsWithGroups.clear();

                for (BaseDevicePermission devicePermission : databaseDevicePermissions) {
                    getDeviceItems(devicePermission.getDeviceId()).add(devicePermission.getSlaveId());
                    getAllDeviceItems(devicePermission.getDeviceId()).add(devicePermission.getSlaveId());
                }

                for (Device device : allDevices) {
                    long groupId = device.getGroupId();
                    while (groupId != 0) {
                        getAllDeviceItems(device.getId()).addAll(getGroupItems(groupId));
                        if (Context.getDeviceManager().getGroupById(groupId) != null) {
                            groupId = Context.getDeviceManager().getGroupById(groupId).getGroupId();
                        } else {
                            groupId = 0;
                        }
                    }
                }

            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }
}
