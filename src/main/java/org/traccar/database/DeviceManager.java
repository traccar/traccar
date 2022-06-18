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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.session.DeviceState;
import org.traccar.storage.StorageException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceManager extends BaseObjectManager<Device> implements IdentityManager {

    private final long dataRefreshDelay;

    private final AtomicLong devicesLastUpdate = new AtomicLong();

    private final Map<Long, DeviceState> deviceStates = new ConcurrentHashMap<>();

    public DeviceManager(Config config, DataManager dataManager) {
        super(dataManager, Device.class);
        dataRefreshDelay = config.getLong(Keys.DATABASE_REFRESH_DELAY) * 1000;
    }

    public void updateDeviceCache(boolean force) {
        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && devicesLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            refreshItems();
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
        cachedDevice.setUniqueId(device.getUniqueId());
    }

    @Override
    protected void removeCachedItem(long deviceId) {
        Device cachedDevice = getById(deviceId);
        if (cachedDevice != null) {
            super.removeCachedItem(deviceId);
        }
    }

    public void updateDeviceStatus(Device device) throws StorageException {
        getDataManager().updateDeviceStatus(device);
        Device cachedDevice = getById(device.getId());
        if (cachedDevice != null) {
            cachedDevice.setStatus(device.getStatus());
        }
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
