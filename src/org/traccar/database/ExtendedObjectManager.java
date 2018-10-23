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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Permission;
import org.traccar.model.BaseModel;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

public abstract class ExtendedObjectManager<T extends BaseModel> extends SimpleObjectManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedObjectManager.class);

    private final Cache<Long, Set<Long>> deviceItems;
    private final Cache<Long, Set<Long>> deviceItemsWithGroups;
    private final Cache<Long, Set<Long>> groupItems;

    protected ExtendedObjectManager(DataManager dataManager, Class<T> baseClass) {
        super(dataManager, baseClass);
        deviceItems = Context.getCacheManager().createCache(
                this.getClass().getSimpleName() + "DeviceItems", new MutableConfiguration<>());
        deviceItemsWithGroups = Context.getCacheManager().createCache(
                this.getClass().getSimpleName() + "DeviceItemsWithGroups", new MutableConfiguration<>());
        groupItems = Context.getCacheManager().createCache(
                this.getClass().getSimpleName() + "GroupItems", new MutableConfiguration<>());
        refreshExtendedPermissions();
    }

    public final Set<Long> getGroupItems(long groupId) {
        if (!groupItems.containsKey(groupId)) {
            return Collections.emptySet();
        }
        return groupItems.get(groupId);
    }

    public final Set<Long> getDeviceItems(long deviceId) {
        if (!deviceItems.containsKey(deviceId)) {
            return Collections.emptySet();
        }
        return deviceItems.get(deviceId);
    }

    public Set<Long> getAllDeviceItems(long deviceId) {
        if (!deviceItemsWithGroups.containsKey(deviceId)) {
            return Collections.emptySet();
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

                Collection<Permission> databaseGroupPermissions =
                        getDataManager().getPermissions(Group.class, getBaseClass());

                Map<Long, Set<Long>> updatedGroupItems = new HashMap<>();
                for (Permission groupPermission : databaseGroupPermissions) {
                    if (!updatedGroupItems.containsKey(groupPermission.getOwnerId())) {
                        updatedGroupItems.put(groupPermission.getOwnerId(), new HashSet<>());
                    }
                    updatedGroupItems.get(groupPermission.getOwnerId()).add(groupPermission.getPropertyId());
                }

                Collection<Permission> databaseDevicePermissions =
                        getDataManager().getPermissions(Device.class, getBaseClass());

                Map<Long, Set<Long>> updatedDeviceItems = new HashMap<>();
                Map<Long, Set<Long>> updatedDeviceItemsWithGroups = new HashMap<>();

                for (Permission devicePermission : databaseDevicePermissions) {
                    if (!updatedDeviceItems.containsKey(devicePermission.getOwnerId())) {
                        updatedDeviceItems.put(devicePermission.getOwnerId(), new HashSet<>());
                    }
                    updatedDeviceItems.get(
                            devicePermission.getOwnerId()).add(devicePermission.getPropertyId());
                    if (!updatedDeviceItemsWithGroups.containsKey(devicePermission.getOwnerId())) {
                        updatedDeviceItemsWithGroups.put(devicePermission.getOwnerId(), new HashSet<>());
                    }
                    updatedDeviceItemsWithGroups.get(
                            devicePermission.getOwnerId()).add(devicePermission.getPropertyId());
                }

                for (Device device : Context.getDeviceManager().getAllDevices()) {
                    long groupId = device.getGroupId();
                    while (groupId != 0 && updatedGroupItems.containsKey(groupId)) {
                        if (!updatedDeviceItemsWithGroups.containsKey(device.getId())) {
                            updatedDeviceItemsWithGroups.put(device.getId(), new HashSet<>());
                        }
                        updatedDeviceItemsWithGroups.get(device.getId()).addAll(updatedGroupItems.get(groupId));
                        Group group = Context.getGroupsManager().getById(groupId);
                        if (group != null) {
                            groupId = group.getGroupId();
                        } else {
                            groupId = 0;
                        }
                    }
                }

                updateCache(groupItems, updatedGroupItems);
                updateCache(deviceItems, updatedDeviceItems);
                updateCache(deviceItemsWithGroups, updatedDeviceItemsWithGroups);

            } catch (SQLException | ClassNotFoundException error) {
                LOGGER.warn("Refresh permissions error", error);
            }
        }
    }

}
