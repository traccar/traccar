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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.helper.Log;
import org.traccar.model.Driver;
import org.traccar.model.BaseModel;

public class DriversManager extends ExtendedObjectManager {

    private final Map<String, Driver> driversByUniqueId = new ConcurrentHashMap<>();

    public DriversManager(DataManager dataManager) {
        super(dataManager, Driver.class);
        refreshItems();
        refreshExtendedPermissions();
    }

    @Override
    public void refreshItems() {
        if (getDataManager() != null) {
            try {
                clearItems();
                for (BaseModel item : getDataManager().getObjects(getBaseClass())) {
                    putItem(item.getId(), item);
                    driversByUniqueId.put(((Driver) item).getUniqueId(), (Driver) item);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserItems();
    }

    @Override
    public void addItem(BaseModel item) throws SQLException {
        super.addItem(item);
        driversByUniqueId.put(((Driver) item).getUniqueId(), (Driver) item);
    }

    @Override
    public void updateItem(BaseModel item) throws SQLException {
        Driver driver = (Driver) item;
        getDataManager().updateObject(driver);
        Driver cachedDriver = (Driver) getById(driver.getId());
        cachedDriver.setName(driver.getName());
        if (!driver.getUniqueId().equals(cachedDriver.getUniqueId())) {
            driversByUniqueId.remove(cachedDriver.getUniqueId());
            cachedDriver.setUniqueId(driver.getUniqueId());
            driversByUniqueId.put(cachedDriver.getUniqueId(), cachedDriver);
        }
        cachedDriver.setAttributes(driver.getAttributes());
    }

    @Override
    public void removeItem(long driverId) throws SQLException {
        Driver cachedDriver = (Driver) getById(driverId);
        getDataManager().removeObject(Driver.class, driverId);
        if (cachedDriver != null) {
            String driverUniqueId = cachedDriver.getUniqueId();
            removeCachedItem(driverId);
            driversByUniqueId.remove(driverUniqueId);
        }
        refreshUserItems();
        refreshExtendedPermissions();
    }

    public Driver getDriverByUniqueId(String uniqueId) {
        return driversByUniqueId.get(uniqueId);
    }
}
