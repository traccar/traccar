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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.DeviceDriver;
import org.traccar.model.Driver;
import org.traccar.model.DriverPermission;
import org.traccar.model.GroupDriver;

public class DriversManager {

    private final DataManager dataManager;

    private final Map<Long, Driver> drivers = new ConcurrentHashMap<>();
    private final Map<String, Driver> driversByUniqueId = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceDrivers = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> deviceDriversWithGroups = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> groupDrivers = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> userDrivers = new ConcurrentHashMap<>();

    public DriversManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshDrivers();
    }

    public Set<Long> getUserDrivers(long userId) {
        if (!userDrivers.containsKey(userId)) {
            userDrivers.put(userId, new HashSet<Long>());
        }
        return userDrivers.get(userId);
    }

    public Set<Long> getGroupDrivers(long groupId) {
        if (!groupDrivers.containsKey(groupId)) {
            groupDrivers.put(groupId, new HashSet<Long>());
        }
        return groupDrivers.get(groupId);
    }

    public Set<Long> getDeviceDrivers(long deviceId) {
        return getDeviceDrivers(deviceDrivers, deviceId);
    }

    public Set<Long> getAllDeviceDrivers(long deviceId) {
        return getDeviceDrivers(deviceDriversWithGroups, deviceId);
    }

    private Set<Long> getDeviceDrivers(Map<Long, Set<Long>> deviceDrivers, long deviceId) {
        if (!deviceDrivers.containsKey(deviceId)) {
            deviceDrivers.put(deviceId, new HashSet<Long>());
        }
        return deviceDrivers.get(deviceId);
    }

    public final void refreshDrivers() {
        if (dataManager != null) {
            try {
                drivers.clear();
                driversByUniqueId.clear();
                for (Driver driver : dataManager.getDrivers()) {
                    drivers.put(driver.getId(), driver);
                    driversByUniqueId.put(driver.getUniqueId(), driver);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserDrivers();
        refresh();
    }

    public final void refreshUserDrivers() {
        if (dataManager != null) {
            try {
                userDrivers.clear();
                for (DriverPermission driverPermission : dataManager.getDriverPermissions()) {
                    getUserDrivers(driverPermission.getUserId()).add(driverPermission.getDriverId());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public final void refresh() {
        if (dataManager != null) {
            try {

                Collection<GroupDriver> databaseGroupDrivers = dataManager.getGroupDrivers();

                groupDrivers.clear();
                for (GroupDriver groupDriver : databaseGroupDrivers) {
                    getGroupDrivers(groupDriver.getGroupId()).add(groupDriver.getDriverId());
                }

                Collection<DeviceDriver> databaseDeviceDrivers = dataManager.getDeviceDrivers();
                Collection<Device> allDevices = Context.getDeviceManager().getAllDevices();

                deviceDrivers.clear();
                deviceDriversWithGroups.clear();

                for (DeviceDriver deviceAttribute : databaseDeviceDrivers) {
                    getDeviceDrivers(deviceAttribute.getDeviceId())
                        .add(deviceAttribute.getDriverId());
                    getAllDeviceDrivers(deviceAttribute.getDeviceId())
                        .add(deviceAttribute.getDriverId());
                }

                for (Device device : allDevices) {
                    long groupId = device.getGroupId();
                    while (groupId != 0) {
                        getAllDeviceDrivers(device.getId()).addAll(getGroupDrivers(groupId));
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

    public void addDriver(Driver driver) throws SQLException {
        dataManager.addDriver(driver);
        drivers.put(driver.getId(), driver);
        driversByUniqueId.put(driver.getUniqueId(), driver);
    }

    public void updateDriver(Driver driver) throws SQLException {
        dataManager.updateDriver(driver);
        Driver cachedDriver = drivers.get(driver.getId());
        cachedDriver.setName(driver.getName());
        if (!driver.getUniqueId().equals(cachedDriver.getUniqueId())) {
            driversByUniqueId.remove(cachedDriver.getUniqueId());
            cachedDriver.setUniqueId(driver.getUniqueId());
            driversByUniqueId.put(cachedDriver.getUniqueId(), cachedDriver);
        }
        cachedDriver.setAttributes(driver.getAttributes());
    }

    public void removeDriver(long driverId) throws SQLException {
        dataManager.removeDriver(driverId);
        if (drivers.containsKey(driverId)) {
            String driverUniqueId = drivers.get(driverId).getUniqueId();
            drivers.remove(driverId);
            driversByUniqueId.remove(driverUniqueId);
        }
        refreshUserDrivers();
        refresh();
    }

    public boolean checkDriver(long userId, long driverId) {
        return getUserDrivers(userId).contains(driverId);
    }

    public Driver getDriver(long id) {
        return drivers.get(id);
    }

    public Driver getDriverByUniqueId(String uniqueId) {
        return driversByUniqueId.get(uniqueId);
    }

    public final Collection<Driver> getDrivers(Set<Long> driverIds) {
        Collection<Driver> result = new LinkedList<>();
        for (long driverId : driverIds) {
            result.add(getDriver(driverId));
        }
        return result;
    }

    public final Set<Long> getAllDrivers() {
        return drivers.keySet();
    }

    public final Set<Long> getManagedDrivers(long userId) {
        Set<Long> drivers = new HashSet<>();
        drivers.addAll(getUserDrivers(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            drivers.addAll(getUserDrivers(managedUserId));
        }
        return drivers;
    }
}
