package org.traccar.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Geofence;
import org.traccar.model.GroupGeofence;
import org.traccar.model.UserDeviceGeofence;
import org.traccar.model.GeofencePermission;

public class GeofenceManager {

    private final DataManager dataManager;

    private final Map<Long, Geofence> geofences = new HashMap<>();
    private final Map<Long, Set<Long>> userGeofences = new HashMap<>();
    private final Map<Long, Set<Long>> groupGeofences = new HashMap<>();

    private final Map<Long, Set<Long>> deviceGeofences = new HashMap<>();
    private final Map<Long, Map<Long, Set<Long>>> userDeviceGeofences = new HashMap<>();

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

    public Set<Long> getAllDeviceGeofences(long deviceId) {
        deviceGeofencesLock.readLock().lock();
        try {
            return getDeviceGeofences(deviceGeofences, deviceId);
        } finally {
            deviceGeofencesLock.readLock().unlock();
        }

    }

    public Set<Long> getUserDeviceGeofences(long userId, long deviceId) {
        deviceGeofencesLock.readLock().lock();
        try {
            return getUserDeviceGeofencesUnlocked(userId, deviceId);
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

    private Set<Long> getUserDeviceGeofencesUnlocked(long userId, long deviceId) {
        if (!userDeviceGeofences.containsKey(userId)) {
            userDeviceGeofences.put(userId, new HashMap<Long, Set<Long>>());
        }
        return getDeviceGeofences(userDeviceGeofences.get(userId), deviceId);
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

                    for (Map.Entry<Long, Map<Long, Set<Long>>> deviceGeofence : userDeviceGeofences.entrySet()) {
                        deviceGeofence.getValue().clear();
                    }
                    userDeviceGeofences.clear();

                    for (UserDeviceGeofence userDeviceGeofence : dataManager.getUserDeviceGeofences()) {
                        getDeviceGeofences(deviceGeofences, userDeviceGeofence.getDeviceId())
                            .add(userDeviceGeofence.getGeofenceId());
                        getUserDeviceGeofencesUnlocked(userDeviceGeofence.getUserId(), userDeviceGeofence.getDeviceId())
                            .add(userDeviceGeofence.getGeofenceId());
                    }
                    for (Device device : dataManager.getAllDevices()) {
                        long groupId = device.getGroupId();
                        while (groupId != 0) {
                            getDeviceGeofences(deviceGeofences, device.getId()).addAll(getGroupGeofences(groupId));
                            groupId = dataManager.getGroupById(groupId).getGroupId();
                        }
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

    public final Collection<Geofence> getUserGeofences(long userId) {
        geofencesLock.readLock().lock();
        try {
            Collection<Geofence> result = new LinkedList<>();
            for (Long geofenceId : getUserGeofencesIds(userId)) {
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

}
