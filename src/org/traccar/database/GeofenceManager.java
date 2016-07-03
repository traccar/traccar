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
import org.traccar.model.Geofence;
import org.traccar.model.GroupGeofence;
import org.traccar.model.Position;
import org.traccar.model.DeviceGeofence;
import org.traccar.model.GeofencePermission;

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

    public GeofenceManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refresh();
    }

    public Set<Long> getUserGeofencesIds(long userId) {
        if (!userGeofences.containsKey(userId)) {
            userGeofences.put(userId, new HashSet<Long>());
        }
        return userGeofences.get(userId);
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

    public final void refresh() {
        if (dataManager != null) {
            try {
                geofencesLock.writeLock().lock();
                groupGeofencesLock.writeLock().lock();
                deviceGeofencesLock.writeLock().lock();
                try {
                    geofences.clear();
                    for (Geofence geofence : dataManager.getGeofences()) {
                        geofences.put(geofence.getId(), geofence);
                    }

                    userGeofences.clear();
                    for (GeofencePermission geofencePermission : dataManager.getGeofencePermissions()) {
                        getUserGeofencesIds(geofencePermission.getUserId()).add(geofencePermission.getGeofenceId());
                    }

                    groupGeofences.clear();
                    for (GroupGeofence groupGeofence : dataManager.getGroupGeofences()) {
                        getGroupGeofences(groupGeofence.getGroupId()).add(groupGeofence.getGeofenceId());
                    }

                    deviceGeofences.clear();
                    deviceGeofencesWithGroups.clear();

                    for (DeviceGeofence deviceGeofence : dataManager.getDeviceGeofences()) {
                        getDeviceGeofences(deviceGeofences, deviceGeofence.getDeviceId())
                            .add(deviceGeofence.getGeofenceId());
                        getDeviceGeofences(deviceGeofencesWithGroups, deviceGeofence.getDeviceId())
                            .add(deviceGeofence.getGeofenceId());
                    }

                    for (Device device : dataManager.getAllDevicesCached()) {
                        long groupId = device.getGroupId();
                        while (groupId != 0) {
                            getDeviceGeofences(deviceGeofencesWithGroups,
                                    device.getId()).addAll(getGroupGeofences(groupId));
                            groupId = dataManager.getGroupById(groupId).getGroupId();
                        }
                        List<Long> deviceGeofenceIds = device.getGeofenceIds();
                        if (deviceGeofenceIds == null) {
                            deviceGeofenceIds = new ArrayList<Long>();
                        } else {
                            deviceGeofenceIds.clear();
                        }
                        Position lastPosition = Context.getConnectionManager().getLastPosition(device.getId());
                        if (lastPosition != null && deviceGeofencesWithGroups.containsKey(device.getId())) {
                            for (Long geofenceId : deviceGeofencesWithGroups.get(device.getId())) {
                                if (getGeofence(geofenceId).getGeometry()
                                        .containsPoint(lastPosition.getLatitude(), lastPosition.getLongitude())) {
                                    deviceGeofenceIds.add(geofenceId);
                                }
                            }
                        }
                        device.setGeofenceIds(deviceGeofenceIds);
                    }

                } finally {
                    geofencesLock.writeLock().unlock();
                    groupGeofencesLock.writeLock().unlock();
                    deviceGeofencesLock.writeLock().unlock();
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

    public final Collection<Geofence> getGeofences(Set<Long> geofencesIds) {
        geofencesLock.readLock().lock();
        try {
            Collection<Geofence> result = new LinkedList<>();
            for (Long geofenceId : geofencesIds) {
                result.add(getGeofence(geofenceId));
            }
            return result;
        } finally {
            geofencesLock.readLock().unlock();
        }
    }

    public final Geofence getGeofence(Long geofenceId) {
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
        List<Long> result = new ArrayList<Long>();
        for (Long geofenceId : getAllDeviceGeofences(position.getDeviceId())) {
            if (getGeofence(geofenceId).getGeometry().containsPoint(position.getLatitude(), position.getLongitude())) {
                result.add(geofenceId);
            }
        }
        return result;
    }

}
