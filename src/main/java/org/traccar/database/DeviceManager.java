/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceState;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.StorageException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceManager extends BaseObjectManager<Device> implements IdentityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManager.class);

    private final Config config;
    private final CacheManager cacheManager;
    private final ConnectionManager connectionManager;
    private final long dataRefreshDelay;

    private Map<String, Device> devicesByUniqueId;
    private final AtomicLong devicesLastUpdate = new AtomicLong();

    private final Map<Long, Position> positions = new ConcurrentHashMap<>();

    private final Map<Long, DeviceState> deviceStates = new ConcurrentHashMap<>();

    public DeviceManager(
            Config config, CacheManager cacheManager, DataManager dataManager, ConnectionManager connectionManager) {
        super(dataManager, Device.class);
        this.config = config;
        this.cacheManager = cacheManager;
        this.connectionManager = connectionManager;
        try {
            writeLock();
            if (devicesByUniqueId == null) {
                devicesByUniqueId = new ConcurrentHashMap<>();
            }
        } finally {
            writeUnlock();
        }
        dataRefreshDelay = config.getLong(Keys.DATABASE_REFRESH_DELAY) * 1000;
        refreshLastPositions();
    }

    public void updateDeviceCache(boolean force) {
        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && devicesLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            refreshItems();
        }
    }

    @Override
    public Device getByUniqueId(String uniqueId) {
        boolean forceUpdate;
        try {
            readLock();
            forceUpdate = !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean(Keys.DATABASE_IGNORE_UNKNOWN);
        } finally {
            readUnlock();
        }
        updateDeviceCache(forceUpdate);
        try {
            readLock();
            return devicesByUniqueId.get(uniqueId);
        } finally {
            readUnlock();
        }
    }

    @Override
    public Set<Long> getAllItems() {
        Set<Long> result = super.getAllItems();
        if (result.isEmpty()) {
            updateDeviceCache(true);
            result = super.getAllItems();
        }
        return result;
    }

    public Collection<Device> getAllDevices() {
        return getItems(getAllItems());
    }

    public Set<Long> getAllUserItems(long userId) {
        return Context.getPermissionsManager().getDevicePermissions(userId);
    }

    public Set<Long> getUserItems(long userId) {
        if (Context.getPermissionsManager() != null) {
            Set<Long> result = new HashSet<>();
            for (long deviceId : Context.getPermissionsManager().getDevicePermissions(userId)) {
                Device device = getById(deviceId);
                if (device != null && !device.getDisabled()) {
                    result.add(deviceId);
                }
            }
            return result;
        } else {
            return new HashSet<>();
        }
    }

    private void addByUniqueId(Device device) {
        try {
            writeLock();
            if (devicesByUniqueId == null) {
                devicesByUniqueId = new ConcurrentHashMap<>();
            }
            devicesByUniqueId.put(device.getUniqueId(), device);
        } finally {
            writeUnlock();
        }
    }

    private void removeByUniqueId(String deviceUniqueId) {
        try {
            writeLock();
            if (devicesByUniqueId != null) {
                devicesByUniqueId.remove(deviceUniqueId);
            }
        } finally {
            writeUnlock();
        }
    }

    @Override
    protected void addNewItem(Device device) {
        super.addNewItem(device);
        addByUniqueId(device);
    }

    @Override
    protected void updateCachedItem(Device device) {
        Device cachedDevice = getById(device.getId());
        cachedDevice.setName(device.getName());
        cachedDevice.setGroupId(device.getGroupId());
        cachedDevice.setCategory(device.getCategory());
        cachedDevice.setContact(device.getContact());
        cachedDevice.setPhone(device.getPhone());
        cachedDevice.setModel(device.getModel());
        cachedDevice.setDisabled(device.getDisabled());
        cachedDevice.setAttributes(device.getAttributes());
        if (!device.getUniqueId().equals(cachedDevice.getUniqueId())) {
            removeByUniqueId(cachedDevice.getUniqueId());
            cachedDevice.setUniqueId(device.getUniqueId());
            addByUniqueId(cachedDevice);
        }
    }

    @Override
    protected void removeCachedItem(long deviceId) {
        Device cachedDevice = getById(deviceId);
        if (cachedDevice != null) {
            String deviceUniqueId = cachedDevice.getUniqueId();
            super.removeCachedItem(deviceId);
            removeByUniqueId(deviceUniqueId);
        }
        positions.remove(deviceId);
    }

    public void updateDeviceStatus(Device device) throws StorageException {
        getDataManager().updateDeviceStatus(device);
        Device cachedDevice = getById(device.getId());
        if (cachedDevice != null) {
            cachedDevice.setStatus(device.getStatus());
        }
    }

    private void refreshLastPositions() {
        if (getDataManager() != null) {
            try {
                for (Position position : getDataManager().getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (StorageException error) {
                LOGGER.warn("Load latest positions error", error);
            }
        }
    }

    public boolean isLatestPosition(Position position) {
        Position lastPosition = getLastPosition(position.getDeviceId());
        return lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0;
    }

    public void updateLatestPosition(Position position) throws StorageException {

        if (isLatestPosition(position)) {

            getDataManager().updateLatestPosition(position);

            Device device = getById(position.getDeviceId());
            if (device != null) {
                device.setPositionId(position.getId());
            }

            positions.put(position.getDeviceId(), position);

            connectionManager.updatePosition(position);
        }
    }

    @Override
    public Position getLastPosition(long deviceId) {
        return positions.get(deviceId);
    }

    public Collection<Position> getInitialState(long userId) {

        List<Position> result = new LinkedList<>();

        if (Context.getPermissionsManager() != null) {
            for (long deviceId : Context.getPermissionsManager().getUserAdmin(userId)
                    ? getAllUserItems(userId) : getUserItems(userId)) {
                if (positions.containsKey(deviceId)) {
                    result.add(positions.get(deviceId));
                }
            }
        }

        return result;
    }

    @Override
    public boolean lookupAttributeBoolean(
            long deviceId, String attributeName, boolean defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        if (result != null) {
            return result instanceof String ? Boolean.parseBoolean((String) result) : (Boolean) result;
        }
        return defaultValue;
    }

    @Override
    public String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        return result != null ? (String) result : defaultValue;
    }

    private Object lookupAttribute(long deviceId, String attributeName, boolean lookupServer, boolean lookupConfig) {
        Object result = null;
        Device device = getById(deviceId);
        if (device != null) {
            result = device.getAttributes().get(attributeName);
            if (result == null) {
                long groupId = device.getGroupId();
                while (groupId > 0) {
                    Group group = cacheManager.getObject(Group.class, device.getGroupId());
                    if (group != null) {
                        result = group.getAttributes().get(attributeName);
                        if (result != null) {
                            break;
                        }
                        groupId = group.getGroupId();
                    } else {
                        groupId = 0;
                    }
                }
            }
            if (result == null && lookupServer) {
                Server server = cacheManager.getServer();
                result = server.getAttributes().get(attributeName);
            }
            if (result == null && lookupConfig) {
                result = Context.getConfig().getString(attributeName);
            }
        }
        return result;
    }

    public DeviceState getDeviceState(long deviceId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            deviceState = new DeviceState();
            deviceStates.put(deviceId, deviceState);
        }
        return deviceState;
    }

    public void setDeviceState(long deviceId, DeviceState deviceState) {
        deviceStates.put(deviceId, deviceState);
    }

}
