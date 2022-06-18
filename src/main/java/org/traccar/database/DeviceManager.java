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

import org.traccar.model.Device;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceManager extends BaseObjectManager<Device> {

    private final AtomicLong devicesLastUpdate = new AtomicLong();

    public DeviceManager(DataManager dataManager) {
        super(dataManager, Device.class);
    }

    public void updateDeviceCache(boolean force) {
        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > 300000L)
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

}
