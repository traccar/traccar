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

import org.traccar.Context;
import org.traccar.model.Driver;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

public class DriversManager extends ExtendedObjectManager<Driver> {

    private Cache<String, Driver> driversByUniqueId;

    public DriversManager(DataManager dataManager) {
        super(dataManager, Driver.class);
    }

    public Cache<String, Driver> getDriversCache() {
        if (driversByUniqueId == null) {
            driversByUniqueId = Context.getCacheManager().createCache(
                    this.getClass().getSimpleName() + "DriversByUniqueId", new MutableConfiguration<>());
        }
        return driversByUniqueId;
    }

    @Override
    protected void addNewItem(Driver driver) {
        super.addNewItem(driver);
        getDriversCache().put(driver.getUniqueId(), driver);
    }

    @Override
    protected void updateCachedItem(Driver driver) {
        Driver cachedDriver = getById(driver.getId());
        cachedDriver.setName(driver.getName());
        if (!driver.getUniqueId().equals(cachedDriver.getUniqueId())) {
            getDriversCache().remove(cachedDriver.getUniqueId());
            cachedDriver.setUniqueId(driver.getUniqueId());
            getDriversCache().put(cachedDriver.getUniqueId(), cachedDriver);
        }
        cachedDriver.setAttributes(driver.getAttributes());
    }

    @Override
    protected void removeCachedItem(long driverId) {
        Driver cachedDriver = getById(driverId);
        if (cachedDriver != null) {
            String driverUniqueId = cachedDriver.getUniqueId();
            super.removeCachedItem(driverId);
            getDriversCache().remove(driverUniqueId);
        }
    }

    public Driver getDriverByUniqueId(String uniqueId) {
        return getDriversCache().get(uniqueId);
    }

}
