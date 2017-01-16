/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.DeviceGeofence;
import org.traccar.model.Geofence;
import org.traccar.model.GeofencePermission;
import org.traccar.model.GroupGeofence;
import org.traccar.model.Position;

public class GeofenceManager {

    private final DataManager dataManager;

    private final Map<Long, Geofence> geofences = new HashMap<>();
    private final Map<Long, Set<Long>> userGeofences = new HashMap<>();
    private final Map<Long, Set<Long>> groupGeofences = new HashMap<>();

    private final Map<Long, Set<Long>> deviceGeofencesWithGroups = new HashMap<>();
    private final Map<Long, Set<Long>> deviceGeofences = new HashMap<>();

    private final ReadWriteLock deviceGeofencesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock geofencesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock groupGeofencesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock userGeofencesLock = new ReentrantReadWriteLock();

    public GeofenceManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshGeofences();
    }

    private Set<Long> getUserGeofences(long userId) {
        if (!userGeofences.containsKey(userId)) {
            userGeofences.put(userId, new HashSet<Long>());
        }
        return userGeofences.get(userId);
    }

    public Set<Long> getUserGeofencesIds(long userId) {
        userGeofencesLock.readLock().lock();
        try {
            return getUserGeofences(userId);
        } finally {
            userGeofencesLock.readLock().unlock();
        }
    }

    private Set<Long> getGroupGeofences(long groupId) {
        if (!groupGeofences.containsKey(groupId)) {
            groupGeofences.put(groupId, new HashSet<Long>());
        }
        return groupGeofences.get(groupId);
    }

    public Set<Long> getGroupGeofencesIds(long groupId) {
        groupGeofencesLock.readLock().lock();
        try {
            return getGroupGeofences(groupId);
        } finally {
            groupGeofencesLock.readLock().unlock();
        }
    }

    public Set<Long> getAllDeviceGeofences(long deviceId) {
        deviceGeofencesLock.readLock().lock();
        try {
            return getDeviceGeofences(deviceGeofencesWithGroups, deviceId);
        } finally {
            deviceGeofencesLock.readLock().unlock();
        }
    }

    public Set<Long> getDeviceGeofencesIds(long deviceId) {
        deviceGeofencesLock.readLock().lock();
        try {
            return getDeviceGeofences(deviceGeofences, deviceId);
        } finally {
            deviceGeofencesLock.readLock().unlock();
        }
    }

    private Set<Long> getDeviceGeofences(Map<Long, Set<Long>> deviceGeofences, long deviceId) {
        if (!deviceGeofences.containsKey(deviceId)) {
            deviceGeofences.put(deviceId, new HashSet<Long>());
        }
        return deviceGeofences.get(deviceId);
    }

    public final void refreshGeofences() {
        if (dataManager != null) {
            try {
                geofencesLock.writeLock().lock();
                try {
                    geofences.clear();
                    for (Geofence geofence : dataManager.getGeofences()) {
                        geofences.put(geofence.getId(), geofence);
                    }
                } finally {
                    geofencesLock.writeLock().unlock();
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserGeofences();
        refresh();
    }

    public final void refreshUserGeofences() {
        if (dataManager != null) {
            try {
                userGeofencesLock.writeLock().lock();
                try {
                    userGeofences.clear();
                    for (GeofencePermission geofencePermission : dataManager.getGeofencePermissions()) {
                        getUserGeofences(geofencePermission.getUserId()).add(geofencePermission.getGeofenceId());
                    }
                } finally {
                    userGeofencesLock.writeLock().unlock();
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public final void refresh() {
        if (dataManager != null) {
            try {

                Collection<GroupGeofence> databaseGroupGeofences = dataManager.getGroupGeofences();
                groupGeofencesLock.writeLock().lock();
                try {
                    groupGeofences.clear();
                    for (GroupGeofence groupGeofence : databaseGroupGeofences) {
                        getGroupGeofences(groupGeofence.getGroupId()).add(groupGeofence.getGeofenceId());
                    }
                } finally {
                    groupGeofencesLock.writeLock().unlock();
                }

                Collection<DeviceGeofence> databaseDeviceGeofences = dataManager.getDeviceGeofences();
                Collection<Device> allDevices = Context.getDeviceManager().getAllDevices();

                groupGeofencesLock.readLock().lock();
                deviceGeofencesLock.writeLock().lock();
                try {
                    deviceGeofences.clear();
                    deviceGeofencesWithGroups.clear();

                    for (DeviceGeofence deviceGeofence : databaseDeviceGeofences) {
                        getDeviceGeofences(deviceGeofences, deviceGeofence.getDeviceId())
                            .add(deviceGeofence.getGeofenceId());
                        getDeviceGeofences(deviceGeofencesWithGroups, deviceGeofence.getDeviceId())
                            .add(deviceGeofence.getGeofenceId());
                    }

                    for (Device device : allDevices) {
                        long groupId = device.getGroupId();
                        while (groupId != 0) {
                            getDeviceGeofences(deviceGeofencesWithGroups,
                                    device.getId()).addAll(getGroupGeofences(groupId));
                            if (Context.getDeviceManager().getGroupById(groupId) != null) {
                                groupId = Context.getDeviceManager().getGroupById(groupId).getGroupId();
                            } else {
                                groupId = 0;
                            }
                        }
                        List<Long> deviceGeofenceIds = device.getGeofenceIds();
                        if (deviceGeofenceIds == null) {
                            deviceGeofenceIds = new ArrayList<>();
                        } else {
                            deviceGeofenceIds.clear();
                        }
                        Position lastPosition = Context.getIdentityManager().getLastPosition(device.getId());
                        if (lastPosition != null && deviceGeofencesWithGroups.containsKey(device.getId())) {
                            for (long geofenceId : deviceGeofencesWithGroups.get(device.getId())) {
                                Geofence geofence = getGeofence(geofenceId);
                                if (geofence != null && geofence.getGeometry()
                                        .containsPoint(lastPosition.getLatitude(), lastPosition.getLongitude())) {
                                    deviceGeofenceIds.add(geofenceId);
                                }
                            }
                        }
                        device.setGeofenceIds(deviceGeofenceIds);
                    }

                } finally {
                    deviceGeofencesLock.writeLock().unlock();
                    groupGeofencesLock.readLock().unlock();
                }

            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public final Collection<Geofence> getAllGeofences() {
        geofencesLock.readLock().lock();
        try {
            return geofences.values();
        } finally {
            geofencesLock.readLock().unlock();
        }
    }

    public final Set<Long> getAllGeofencesIds() {
        geofencesLock.readLock().lock();
        try {
            return geofences.keySet();
        } finally {
            geofencesLock.readLock().unlock();
        }
    }

    public final Set<Long> getManagedGeofencesIds(long userId) {
        Set<Long> geofences = new HashSet<>();
        geofences.addAll(getUserGeofencesIds(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            geofences.addAll(getUserGeofencesIds(managedUserId));
        }
        return geofences;
    }

    public final Collection<Geofence> getGeofences(Set<Long> geofencesIds) {
        geofencesLock.readLock().lock();
        try {
            Collection<Geofence> result = new LinkedList<>();
            for (long geofenceId : geofencesIds) {
                result.add(getGeofence(geofenceId));
            }
            return result;
        } finally {
            geofencesLock.readLock().unlock();
        }
    }

    public final Geofence getGeofence(long geofenceId) {
        geofencesLock.readLock().lock();
        try {
            return geofences.get(geofenceId);
        } finally {
            geofencesLock.readLock().unlock();
        }
    }

    public final void updateGeofence(Geofence geofence) {
        geofencesLock.writeLock().lock();
        try {
            geofences.put(geofence.getId(), geofence);
        } finally {
            geofencesLock.writeLock().unlock();
        }
        try {
            dataManager.updateGeofence(geofence);
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public boolean checkGeofence(long userId, long geofenceId) {
        return getUserGeofencesIds(userId).contains(geofenceId);
    }

    public List<Long> getCurrentDeviceGeofences(Position position) {
        List<Long> result = new ArrayList<>();
        for (long geofenceId : getAllDeviceGeofences(position.getDeviceId())) {
            if (getGeofence(geofenceId).getGeometry().containsPoint(position.getLatitude(), position.getLongitude())) {
                result.add(geofenceId);
            }
        }
        return result;
    }

}
