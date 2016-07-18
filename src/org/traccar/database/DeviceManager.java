/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.traccar.Config;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class DeviceManager implements IdentityManager {

    private final Config config;
    private final DataManager dataManager;
    private final long dataRefreshDelay;

    private final ReadWriteLock devicesLock = new ReentrantReadWriteLock();
    private final Map<Long, Device> devicesById = new HashMap<>();
    private final Map<String, Device> devicesByUniqueId = new HashMap<>();
    private long devicesLastUpdate;

    private final Map<Long, Position> positions = new ConcurrentHashMap<>();

    public DeviceManager(DataManager dataManager) {
        this.dataManager = dataManager;
        this.config = Context.getConfig();
        dataRefreshDelay = config.getLong("database.refreshDelay", DataManager.DEFAULT_REFRESH_DELAY) * 1000;
        if (dataManager != null) {
            try {
                for (Position position : dataManager.getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    private void updateDeviceCache(boolean force) throws SQLException {
        boolean needWrite;
        devicesLock.readLock().lock();
        try {
            needWrite = force || System.currentTimeMillis() - devicesLastUpdate > dataRefreshDelay;
        } finally {
            devicesLock.readLock().unlock();
        }

        if (needWrite) {
            devicesLock.writeLock().lock();
            try {
                if (force || System.currentTimeMillis() - devicesLastUpdate > dataRefreshDelay) {
                    devicesById.clear();
                    devicesByUniqueId.clear();
                    GeofenceManager geofenceManager = Context.getGeofenceManager();
                    for (Device device : dataManager.getAllDevices()) {
                        devicesById.put(device.getId(), device);
                        devicesByUniqueId.put(device.getUniqueId(), device);
                        if (geofenceManager != null) {
                            Position lastPosition = getLastPosition(device.getId());
                            if (lastPosition != null) {
                                device.setGeofenceIds(geofenceManager.getCurrentDeviceGeofences(lastPosition));
                            }
                        }
                    }
                    devicesLastUpdate = System.currentTimeMillis();
                }
            } finally {
                devicesLock.writeLock().unlock();
            }
        }
    }

    @Override
    public Device getDeviceById(long id) {
        boolean forceUpdate;
        devicesLock.readLock().lock();
        try {
            forceUpdate = !devicesById.containsKey(id);
        } finally {
            devicesLock.readLock().unlock();
        }

        try {
            updateDeviceCache(forceUpdate);
        } catch (SQLException e) {
            Log.warning(e);
        }

        devicesLock.readLock().lock();
        try {
            return devicesById.get(id);
        } finally {
            devicesLock.readLock().unlock();
        }
    }

    @Override
    public Device getDeviceByUniqueId(String uniqueId) throws SQLException {
        boolean forceUpdate;
        devicesLock.readLock().lock();
        try {
            forceUpdate = !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean("database.ignoreUnknown");
        } finally {
            devicesLock.readLock().unlock();
        }

        updateDeviceCache(forceUpdate);

        devicesLock.readLock().lock();
        try {
            return devicesByUniqueId.get(uniqueId);
        } finally {
            devicesLock.readLock().unlock();
        }
    }

    public Collection<Device> getAllDevices() {
        boolean forceUpdate;
        devicesLock.readLock().lock();
        try {
            forceUpdate = devicesById.isEmpty();
        } finally {
            devicesLock.readLock().unlock();
        }

        try {
            updateDeviceCache(forceUpdate);
        } catch (SQLException e) {
            Log.warning(e);
        }

        devicesLock.readLock().lock();
        try {
            return devicesById.values();
        } finally {
            devicesLock.readLock().unlock();
        }
    }

    public Collection<Device> getDevices(long userId) throws SQLException {
        Collection<Device> devices = new ArrayList<>();
        devicesLock.readLock().lock();
        try {
            for (long id : Context.getPermissionsManager().getDevicePermissions(userId)) {
                    devices.add(devicesById.get(id));
            }
        } finally {
            devicesLock.readLock().unlock();
        }
        return devices;
    }

    public void addDevice(Device device) throws SQLException {
        dataManager.addDevice(device);

        devicesLock.writeLock().lock();
        try {
            devicesById.put(device.getId(), device);
            devicesByUniqueId.put(device.getUniqueId(), device);
        } finally {
            devicesLock.writeLock().unlock();
        }
    }

    public void updateDevice(Device device) throws SQLException {
        dataManager.updateDevice(device);

        devicesLock.writeLock().lock();
        try {
            devicesById.put(device.getId(), device);
            devicesByUniqueId.put(device.getUniqueId(), device);
        } finally {
            devicesLock.writeLock().unlock();
        }
    }

    public void updateDeviceStatus(Device device) throws SQLException {
        dataManager.updateDeviceStatus(device);

        devicesLock.writeLock().lock();
        try {
            if (devicesById.containsKey(device.getId())) {
                Device cachedDevice = devicesById.get(device.getId());
                cachedDevice.setStatus(device.getStatus());
                cachedDevice.setMotion(device.getMotion());
            }
        } finally {
            devicesLock.writeLock().unlock();
        }
    }

    public void removeDevice(long deviceId) throws SQLException {
        dataManager.removeDevice(deviceId);

        devicesLock.writeLock().lock();
        try {
            if (devicesById.containsKey(deviceId)) {
                String deviceUniqueId = devicesById.get(deviceId).getUniqueId();
                devicesById.remove(deviceId);
                devicesByUniqueId.remove(deviceUniqueId);
            }
        } finally {
            devicesLock.writeLock().unlock();
        }

        positions.remove(deviceId);
    }

    public void updateLatestPosition(Position position) throws SQLException {

        Position lastPosition = getLastPosition(position.getDeviceId());
        if (lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) > 0) {

            dataManager.updateLatestPosition(position);

            devicesLock.writeLock().lock();
            try {
                if (devicesById.containsKey(position.getDeviceId())) {
                    devicesById.get(position.getDeviceId()).setPositionId(position.getId());
                }
            } finally {
                devicesLock.writeLock().unlock();
            }

            positions.put(position.getDeviceId(), position);

            if (Context.getConnectionManager() != null) {
                Context.getConnectionManager().updatePosition(position);
            }
        }
    }

    @Override
    public Position getLastPosition(long deviceId) {
        return positions.get(deviceId);
    }

    public Collection<Position> getInitialState(long userId) {

        List<Position> result = new LinkedList<>();

        if (Context.getPermissionsManager() != null) {
            for (long deviceId : Context.getPermissionsManager().getDevicePermissions(userId)) {
                if (positions.containsKey(deviceId)) {
                    result.add(positions.get(deviceId));
                }
            }
        }

        return result;
    }
}
