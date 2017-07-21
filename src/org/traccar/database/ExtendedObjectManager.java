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
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.BaseModel;

public abstract class ExtendedObjectManager extends SimpleObjectManager {

    private final Map<Long, Set<Long>> deviceItems = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceItemsWithGroups = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> groupItems = new ConcurrentHashMap<>();

    protected ExtendedObjectManager(DataManager dataManager,
            Class<? extends BaseModel> baseClass) {
        super(dataManager, baseClass);
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

                Collection<Map<String, Long>> databaseGroupPermissions =
                        getDataManager().getPermissions(Group.class, getBaseClass());

                clearGroupItems();
                for (Map<String, Long> groupPermission : databaseGroupPermissions) {
                    getGroupItems(groupPermission.get(DataManager.makeNameId(Group.class)))
                            .add(groupPermission.get(getBaseClassIdName()));
                }

                Collection<Map<String, Long>> databaseDevicePermissions =
                        getDataManager().getPermissions(Device.class, getBaseClass());
                Collection<Device> allDevices = Context.getDeviceManager().getAllDevices();

                clearDeviceItems();
                deviceItemsWithGroups.clear();

                for (Map<String, Long> devicePermission : databaseDevicePermissions) {
                    getDeviceItems(devicePermission.get(DataManager.makeNameId(Device.class)))
                            .add(devicePermission.get(getBaseClassIdName()));
                    getAllDeviceItems(devicePermission.get(DataManager.makeNameId(Device.class)))
                            .add(devicePermission.get(getBaseClassIdName()));
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
