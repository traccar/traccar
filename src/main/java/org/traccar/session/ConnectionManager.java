/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Protocol;
import org.traccar.broadcast.BroadcastInterface;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.DeviceLookupService;
import org.traccar.database.DeviceUpdateManager;
import org.traccar.database.NotificationManager;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Group;
import org.traccar.model.LogRecord;
import org.traccar.model.ObjectOperation;
import org.traccar.model.Position;
import org.traccar.model.RevokedToken;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Singleton
public class ConnectionManager implements BroadcastInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final long deviceTimeout;
    private final boolean showUnknownDevices;

    private record UnknownEntry(String uniqueId, long timestamp) {}

    private final Map<Long, DeviceSession> sessionsByDeviceId = new ConcurrentHashMap<>();
    private final Map<ConnectionKey, Map<String, DeviceSession>> sessionsByEndpoint = new ConcurrentHashMap<>();
    private final Map<ConnectionKey, UnknownEntry> unknownByEndpoint = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastSeenByDeviceId = new ConcurrentHashMap<>();

    private final Config config;
    private final CacheManager cacheManager;
    private final Storage storage;
    private final DeviceUpdateManager deviceUpdateManager;
    private final NotificationManager notificationManager;
    private final BroadcastService broadcastService;
    private final DeviceLookupService deviceLookupService;

    private final Map<Long, Set<UpdateListener>> listeners = new HashMap<>();
    private final Map<Long, Set<Long>> userDevices = new HashMap<>();
    private final Map<Long, Set<Long>> deviceUsers = new HashMap<>();

    @Inject
    public ConnectionManager(
            Config config, CacheManager cacheManager, Storage storage,
            NotificationManager notificationManager, BroadcastService broadcastService,
            DeviceLookupService deviceLookupService, DeviceUpdateManager deviceUpdateManager) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.storage = storage;
        this.deviceUpdateManager = deviceUpdateManager;
        this.notificationManager = notificationManager;
        this.broadcastService = broadcastService;
        this.deviceLookupService = deviceLookupService;
        deviceTimeout = config.getLong(Keys.STATUS_TIMEOUT);
        showUnknownDevices = config.getBoolean(Keys.WEB_SHOW_UNKNOWN_DEVICES);
        broadcastService.registerListener(this);
    }

    public void sweepIdleSessions() {
        long cutoff = System.currentTimeMillis() - deviceTimeout * 1000;
        for (var entry : lastSeenByDeviceId.entrySet()) {
            if (entry.getValue() < cutoff) {
                deviceUnknown(entry.getKey());
            }
        }
        unknownByEndpoint.values().removeIf(entry -> entry.timestamp() < cutoff);
    }

    public DeviceSession getDeviceSession(long deviceId) {
        return sessionsByDeviceId.get(deviceId);
    }

    private DeviceSession checkDeviceSession(DeviceSession deviceSession) {
        deviceSession.setLastUpdate(System.currentTimeMillis());
        cacheManager.getObject(Device.class, deviceSession.getDeviceId()).checkDisabled();
        return deviceSession;
    }

    public DeviceSession getDeviceSession(
            Protocol protocol, Channel channel, SocketAddress remoteAddress,
            String... uniqueIds) throws Exception {

        ConnectionKey connectionKey = new ConnectionKey(channel, remoteAddress);
        Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.get(connectionKey);

        uniqueIds = Arrays.stream(uniqueIds).filter(Objects::nonNull).toArray(String[]::new);
        if (uniqueIds.length > 0) {
            if (endpointSessions != null) {
                for (String uniqueId : uniqueIds) {
                    DeviceSession deviceSession = endpointSessions.get(uniqueId);
                    if (deviceSession != null) {
                        return checkDeviceSession(deviceSession);
                    }
                }
            }
        } else {
            if (endpointSessions != null) {
                DeviceSession deviceSession = endpointSessions.values().stream().findAny().orElse(null);
                if (deviceSession != null) {
                    return checkDeviceSession(deviceSession);
                }
                return null;
            }
            return null;
        }

        Device device = deviceLookupService.lookup(uniqueIds);

        String firstUniqueId = uniqueIds[0];
        if (device == null && config.getBoolean(Keys.DATABASE_REGISTER_UNKNOWN)) {
            if (firstUniqueId.matches(config.getString(Keys.DATABASE_REGISTER_UNKNOWN_REGEX))) {
                device = addUnknownDevice(firstUniqueId);
                if (device != null) {
                    cacheManager.invalidateObject(true, Device.class, device.getId(), ObjectOperation.ADD);
                }
            }
        }

        if (device != null) {
            unknownByEndpoint.remove(connectionKey);

            DeviceSession oldSession = sessionsByDeviceId.remove(device.getId());
            if (oldSession != null) {
                Map<String, DeviceSession> oldEndpointSessions = sessionsByEndpoint.get(oldSession.getConnectionKey());
                if (oldEndpointSessions != null && oldEndpointSessions.size() > 1) {
                    oldEndpointSessions.remove(device.getUniqueId());
                } else {
                    sessionsByEndpoint.remove(oldSession.getConnectionKey());
                }
            }

            DeviceSession deviceSession = new DeviceSession(
                    device.getId(), device.getUniqueId(), device.getModel(), protocol, channel, remoteAddress);
            sessionsByEndpoint
                    .computeIfAbsent(connectionKey, k -> new ConcurrentHashMap<>())
                    .put(device.getUniqueId(), deviceSession);
            sessionsByDeviceId.put(device.getId(), deviceSession);

            cacheManager.addDevice(device.getId(), device, connectionKey);
            if (oldSession != null) {
                cacheManager.removeDevice(device.getId(), oldSession.getConnectionKey());
            }

            return deviceSession;
        } else {
            unknownByEndpoint.put(connectionKey, new UnknownEntry(firstUniqueId, System.currentTimeMillis()));
            LOGGER.warn("Unknown device - " + String.join(" ", uniqueIds)
                    + " (" + ((InetSocketAddress) remoteAddress).getHostString() + ")");
            return null;
        }
    }

    private Device addUnknownDevice(String uniqueId) {
        Device device = new Device();
        device.setName(uniqueId);
        device.setUniqueId(uniqueId);
        device.setCategory(config.getString(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_CATEGORY));

        long defaultGroupId = config.getLong(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_GROUP_ID);
        if (defaultGroupId != 0) {
            device.setGroupId(defaultGroupId);
        }

        try {
            device.setId(storage.addObject(device, new Request(new Columns.Exclude("id"))));
            LOGGER.info("Automatically registered " + uniqueId);
            return device;
        } catch (StorageException e) {
            LOGGER.warn("Automatic registration failed", e);
            return null;
        }
    }

    public void deviceDisconnected(Channel channel, boolean supportsOffline) {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress != null) {
            ConnectionKey connectionKey = new ConnectionKey(channel, remoteAddress);
            Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.remove(connectionKey);
            if (endpointSessions != null) {
                for (DeviceSession deviceSession : endpointSessions.values()) {
                    boolean removed = sessionsByDeviceId.remove(deviceSession.getDeviceId(), deviceSession);
                    if (supportsOffline && removed) {
                        updateDevice(deviceSession.getDeviceId(), Device.STATUS_OFFLINE, null);
                    }
                    cacheManager.removeDevice(deviceSession.getDeviceId(), connectionKey);
                }
            }
            unknownByEndpoint.remove(connectionKey);
        }
    }

    public void deviceUnknown(long deviceId) {
        updateDevice(deviceId, Device.STATUS_UNKNOWN, null);
        removeDeviceSession(deviceId);
    }

    private void removeDeviceSession(long deviceId) {
        lastSeenByDeviceId.remove(deviceId);
        DeviceSession deviceSession = sessionsByDeviceId.remove(deviceId);
        if (deviceSession != null) {
            ConnectionKey connectionKey = deviceSession.getConnectionKey();
            cacheManager.removeDevice(deviceId, connectionKey);
            sessionsByEndpoint.computeIfPresent(connectionKey, (e, sessions) -> {
                sessions.remove(deviceSession.getUniqueId());
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    public void updateDevice(long deviceId, String status, Date time) {
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            try {
                device = storage.getObject(Device.class, new Request(
                        new Columns.All(), new Condition.Equals("id", deviceId)));
            } catch (StorageException e) {
                LOGGER.warn("Failed to get device", e);
            }
            if (device == null) {
                return;
            }
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);

        if (Device.STATUS_ONLINE.equals(status)) {
            lastSeenByDeviceId.put(deviceId, System.currentTimeMillis());
        } else {
            lastSeenByDeviceId.remove(deviceId);
        }

        if (!status.equals(oldStatus) && isStatusEventEnabled(deviceId)) {
            String eventType = switch (status) {
                case Device.STATUS_ONLINE -> Event.TYPE_DEVICE_ONLINE;
                case Device.STATUS_UNKNOWN -> Event.TYPE_DEVICE_UNKNOWN;
                default -> Event.TYPE_DEVICE_OFFLINE;
            };
            notificationManager.updateEvents(Collections.singletonMap(new Event(eventType, deviceId), null));
        }

        if (time != null) {
            device.setLastUpdate(time);
        }

        try {
            var values = new HashMap<String, Object>();
            values.put("status", status);
            values.put("lastUpdate", device.getLastUpdate());
            deviceUpdateManager.update(deviceId, values);
        } catch (StorageException e) {
            LOGGER.warn("Update device status error", e);
        }

        updateDevice(true, device);
    }

    private boolean isStatusEventEnabled(long deviceId) {
        try {
            return cacheManager.withDevice(deviceId,
                    () -> AttributeUtil.lookup(cacheManager, Keys.EVENT_STATUS_ENABLE, deviceId));
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void sendKeepalive() {
        var iterator = listeners.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var userListeners = entry.getValue();
            if (userListeners.isEmpty()) {
                long userId = entry.getKey();
                iterator.remove();
                var devices = userDevices.remove(userId);
                if (devices != null) {
                    devices.forEach(deviceId -> deviceUsers.computeIfPresent(deviceId, (id, userIds) -> {
                        userIds.remove(userId);
                        return userIds.isEmpty() ? null : userIds;
                    }));
                }
            } else {
                for (UpdateListener listener : userListeners) {
                    listener.onKeepalive();
                }
            }
        }
    }

    @Override
    public synchronized void updateDevice(boolean local, Device device) {
        if (local) {
            broadcastService.updateDevice(true, device);
        } else if (Device.STATUS_ONLINE.equals(device.getStatus())) {
            removeDeviceSession(device.getId());
        }
        for (long userId : deviceUsers.getOrDefault(device.getId(), Collections.emptySet())) {
            Set<UpdateListener> userListeners = listeners.get(userId);
            if (userListeners != null) {
                for (UpdateListener listener : userListeners) {
                    listener.onUpdateDevice(device);
                }
            }
        }
    }

    @Override
    public synchronized void updatePosition(boolean local, Position position) {
        if (local) {
            broadcastService.updatePosition(true, position);
        }
        for (long userId : deviceUsers.getOrDefault(position.getDeviceId(), Collections.emptySet())) {
            Set<UpdateListener> userListeners = listeners.get(userId);
            if (userListeners != null) {
                for (UpdateListener listener : userListeners) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    @Override
    public synchronized void updateEvent(boolean local, long userId, Event event) {
        if (local) {
            broadcastService.updateEvent(true, userId, event);
        }
        Set<UpdateListener> userListeners = listeners.get(userId);
        if (userListeners != null) {
            for (UpdateListener listener : userListeners) {
                listener.onUpdateEvent(event);
            }
        }
    }

    @Override
    public synchronized <T extends BaseModel> void invalidateObject(
            boolean local, Class<T> clazz, long id, ObjectOperation operation) throws StorageException {
        if (clazz.equals(RevokedToken.class) && operation == ObjectOperation.ADD) {
            listeners.values().stream().flatMap(Set::stream).forEach(listener -> listener.onRevokeToken(id));
        } else if ((clazz.equals(Device.class) || clazz.equals(Group.class)) && operation != ObjectOperation.ADD) {
            updateAllUserDevices();
        } else if (clazz.equals(User.class) && operation != ObjectOperation.ADD) {
            var userListeners = listeners.get(id);
            if (userListeners != null && !userListeners.isEmpty()) {
                User user = operation == ObjectOperation.DELETE ? null : storage.getObject(User.class, new Request(
                        new Columns.All(), new Condition.Equals("id", id)));
                for (UpdateListener listener : userListeners) {
                    listener.onUpdateUser(user);
                }
            }
        }
    }

    @Override
    public synchronized <T1 extends BaseModel, T2 extends BaseModel> void invalidatePermission(
            boolean local, Class<T1> clazz1, long id1, Class<T2> clazz2, long id2, boolean link)
            throws StorageException {
        if (clazz1.equals(User.class) && clazz2.equals(Group.class)) {
            updateAllUserDevices();
        } else if (clazz1.equals(User.class) && clazz2.equals(Device.class) && listeners.containsKey(id1)) {
            updateUserDevices(id1);
        }
    }

    private void updateAllUserDevices() throws StorageException {
        for (var entry : listeners.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                updateUserDevices(entry.getKey());
            }
        }
    }

    private void updateUserDevices(long userId) throws StorageException {
        var devices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"), new Condition.Permission(User.class, userId, Device.class)));
        var deviceIds = devices.stream().map(BaseModel::getId).collect(Collectors.toSet());
        for (long deviceId : userDevices.getOrDefault(userId, Collections.emptySet())) {
            if (!deviceIds.contains(deviceId)) {
                deviceUsers.computeIfPresent(deviceId, (id, userIds) -> {
                    userIds.remove(userId);
                    return userIds.isEmpty() ? null : userIds;
                });
            }
        }
        deviceIds.forEach(deviceId -> deviceUsers.computeIfAbsent(deviceId, id -> new HashSet<>()).add(userId));
        userDevices.put(userId, deviceIds);
    }

    public synchronized void updateLog(LogRecord record) {
        var sessions = sessionsByEndpoint.getOrDefault(record.getConnectionKey(), Map.of());
        if (sessions.isEmpty()) {
            UnknownEntry unknown = unknownByEndpoint.get(record.getConnectionKey());
            if (unknown != null && showUnknownDevices) {
                record.setUniqueId(unknown.uniqueId());
                listeners.values().stream()
                        .flatMap(Set::stream)
                        .forEach((listener) -> listener.onUpdateLog(record));
            }
        } else if (sessions.size() == 1) {
            var firstEntry = sessions.entrySet().iterator().next();
            record.setUniqueId(firstEntry.getKey());
            record.setDeviceId(firstEntry.getValue().getDeviceId());
            for (long userId : deviceUsers.getOrDefault(record.getDeviceId(), Set.of())) {
                for (UpdateListener listener : listeners.getOrDefault(userId, Set.of())) {
                    listener.onUpdateLog(record);
                }
            }
        }
    }

    public interface UpdateListener {
        void onKeepalive();

        void onUpdateUser(User user);

        void onRevokeToken(long tokenId);

        void onUpdateDevice(Device device);

        void onUpdatePosition(Position position);

        void onUpdateEvent(Event event);

        void onUpdateLog(LogRecord record);
    }

    public synchronized void addListener(long userId, UpdateListener listener) throws StorageException {
        var set = listeners.get(userId);
        if (set == null) {
            set = new CopyOnWriteArraySet<>();
            listeners.put(userId, set);

            updateUserDevices(userId);
        }
        set.add(listener);
    }

    public synchronized void removeListener(long userId, UpdateListener listener) {
        var set = listeners.get(userId);
        if (set != null) {
            set.remove(listener);
        }
    }

}
