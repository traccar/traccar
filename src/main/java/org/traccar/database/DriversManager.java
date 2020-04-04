/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.model.Driver;

public class DriversManager extends ExtendedObjectManager<Driver> {

    private Map<String, Driver> driversByUniqueId;

    public DriversManager(DataManager dataManager) {
        super(dataManager, Driver.class);
        try {
            writeLock();
            if (driversByUniqueId == null) {
                driversByUniqueId = new ConcurrentHashMap<>();
            }
        } finally {
            writeUnlock();
        }
    }

    private void addByUniqueId(Driver driver) {
        try {
            writeLock();
            if (driversByUniqueId == null) {
                driversByUniqueId = new ConcurrentHashMap<>();
            }
            driversByUniqueId.put(driver.getUniqueId(), driver);
        } finally {
            writeUnlock();
        }
    }

    private void removeByUniqueId(String driverUniqueId) {
        try {
            writeLock();
            if (driversByUniqueId == null) {
                driversByUniqueId = new ConcurrentHashMap<>();
            }
            driversByUniqueId.remove(driverUniqueId);
        } finally {
            writeUnlock();
        }
    }

    @Override
    protected void addNewItem(Driver driver) {
        super.addNewItem(driver);
        addByUniqueId(driver);
    }

    @Override
    protected void updateCachedItem(Driver driver) {
        Driver cachedDriver = getById(driver.getId());
        cachedDriver.setName(driver.getName());
        if (!driver.getUniqueId().equals(cachedDriver.getUniqueId())) {
            removeByUniqueId(cachedDriver.getUniqueId());
            cachedDriver.setUniqueId(driver.getUniqueId());
            addByUniqueId(cachedDriver);
        }
        cachedDriver.setAttributes(driver.getAttributes());
    }

    @Override
    protected void removeCachedItem(long driverId) {
        Driver cachedDriver = getById(driverId);
        if (cachedDriver != null) {
            String driverUniqueId = cachedDriver.getUniqueId();
            super.removeCachedItem(driverId);
            removeByUniqueId(driverUniqueId);
        }
    }

    public Driver getDriverByUniqueId(String uniqueId) {
        try {
            readLock();
            return driversByUniqueId.get(uniqueId);
        } finally {
            readUnlock();
        }
    }
}
