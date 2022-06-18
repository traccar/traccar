/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Main;
import org.traccar.Protocol;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.NotificationManager;
import org.traccar.handler.events.MotionEventHandler;
import org.traccar.handler.events.OverspeedEventHandler;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private final long deviceTimeout;
    private final boolean updateDeviceState;

    private final Map<Long, DeviceSession> sessionsByDeviceId = new ConcurrentHashMap<>();
    private final Map<Endpoint, Map<String, DeviceSession>> sessionsByEndpoint = new ConcurrentHashMap<>();

    private final Map<Long, DeviceState> deviceStates = new ConcurrentHashMap<>();

    private final Config config;
    private final CacheManager cacheManager;
    private final Storage storage;
    private final NotificationManager notificationManager;
    private final Timer timer;

    private final Map<Long, Set<UpdateListener>> listeners = new ConcurrentHashMap<>();
    private final Map<Long, Timeout> timeouts = new ConcurrentHashMap<>();

    @Inject
    public ConnectionManager(
            Config config, CacheManager cacheManager, Storage storage,
            NotificationManager notificationManager, Timer timer) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.storage = storage;
        this.notificationManager = notificationManager;
        this.timer = timer;
        deviceTimeout = config.getLong(Keys.STATUS_TIMEOUT) * 1000;
        updateDeviceState = config.getBoolean(Keys.STATUS_UPDATE_DEVICE_STATE);
    }

    public DeviceSession getDeviceSession(long deviceId) {
        return sessionsByDeviceId.get(deviceId);
    }

    public DeviceSession getDeviceSession(
            Protocol protocol, Channel channel, SocketAddress remoteAddress,
            String... uniqueIds) throws StorageException {

        Endpoint endpoint = new Endpoint(channel, remoteAddress);
        Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.getOrDefault(
                endpoint, new ConcurrentHashMap<>());
        if (uniqueIds.length > 0) {
            for (String uniqueId : uniqueIds) {
                DeviceSession deviceSession = endpointSessions.get(uniqueId);
                if (deviceSession != null) {
                    return deviceSession;
                }
            }
        } else {
            return endpointSessions.values().stream().findAny().orElse(null);
        }

        Device device = null;
        try {
            for (String uniqueId : uniqueIds) {
                device = storage.getObject(Device.class, new Request(
                        new Columns.All(), new Condition.Equals("uniqueId", "uniqueId", uniqueId)));
                if (device != null) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Find device error", e);
        }

        if (device == null && config.getBoolean(Keys.DATABASE_REGISTER_UNKNOWN)) {
            device = addUnknownDevice(uniqueIds[0]);
        }

        if (device != null && !device.getDisabled()) {
            DeviceSession oldSession = sessionsByDeviceId.remove(device.getId());
            if (oldSession != null) {
                Endpoint oldEndpoint = new Endpoint(oldSession.getChannel(), oldSession.getRemoteAddress());
                Map<String, DeviceSession> oldEndpointSessions = sessionsByEndpoint.get(oldEndpoint);
                if (oldEndpointSessions != null && oldEndpointSessions.size() > 1) {
                    oldEndpointSessions.remove(device.getUniqueId());
                } else {
                    sessionsByEndpoint.remove(oldEndpoint);
                }
            }

            DeviceSession deviceSession = new DeviceSession(
                    device.getId(), device.getUniqueId(), protocol, channel, remoteAddress);
            endpointSessions.put(device.getUniqueId(), deviceSession);
            sessionsByEndpoint.put(endpoint, endpointSessions);
            sessionsByDeviceId.put(device.getId(), deviceSession);
            cacheManager.addDevice(device.getId());

            return deviceSession;
        } else {
            LOGGER.warn((device == null ? "Unknown" : "Disabled") + " device - " + String.join(" ", uniqueIds)
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

    public void deviceDisconnected(Channel channel) {
        Endpoint endpoint = new Endpoint(channel, channel.remoteAddress());
        Map<String, DeviceSession> endpointSessions = sessionsByEndpoint.remove(endpoint);
        if (endpointSessions != null) {
            for (DeviceSession deviceSession : endpointSessions.values()) {
                updateDevice(deviceSession.getDeviceId(), Device.STATUS_OFFLINE, null);
                sessionsByDeviceId.remove(deviceSession.getDeviceId());
                cacheManager.removeDevice(deviceSession.getDeviceId());
            }
        }
    }

    public void deviceUnknown(long deviceId) {
        updateDevice(deviceId, Device.STATUS_UNKNOWN, null);
        DeviceSession deviceSession = sessionsByDeviceId.remove(deviceId);
        cacheManager.removeDevice(deviceId);
        if (deviceSession != null) {
            Endpoint endpoint = new Endpoint(deviceSession.getChannel(), deviceSession.getRemoteAddress());
            sessionsByEndpoint.computeIfPresent(endpoint, (e, sessions) -> {
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
                        new Columns.All(), new Condition.Equals("id", "id", deviceId)));
            } catch (StorageException e) {
                LOGGER.warn("Failed to get device", e);
            }
            if (device == null) {
                return;
            }
        }

        String oldStatus = device.getStatus();
        device.setStatus(status);

        if (!status.equals(oldStatus)) {
            String eventType;
            Map<Event, Position> events = new HashMap<>();
            switch (status) {
                case Device.STATUS_ONLINE:
                    eventType = Event.TYPE_DEVICE_ONLINE;
                    break;
                case Device.STATUS_UNKNOWN:
                    eventType = Event.TYPE_DEVICE_UNKNOWN;
                    if (updateDeviceState) {
                        events.putAll(updateDeviceState(deviceId));
                    }
                    break;
                default:
                    eventType = Event.TYPE_DEVICE_OFFLINE;
                    if (updateDeviceState) {
                        events.putAll(updateDeviceState(deviceId));
                    }
                    break;
            }
            events.put(new Event(eventType, deviceId), null);
            notificationManager.updateEvents(events);
        }

        Timeout timeout = timeouts.remove(deviceId);
        if (timeout != null) {
            timeout.cancel();
        }

        if (time != null) {
            device.setLastUpdate(time);
        }

        if (status.equals(Device.STATUS_ONLINE)) {
            timeouts.put(deviceId, timer.newTimeout(timeout1 -> {
                if (!timeout1.isCancelled()) {
                    deviceUnknown(deviceId);
                }
            }, deviceTimeout, TimeUnit.MILLISECONDS));
        }

        try {
            storage.updateObject(device, new Request(
                    new Columns.Include("lastUpdate"),
                    new Condition.Equals("id", "id")));
        } catch (StorageException e) {
            LOGGER.warn("Update device status error", e);
        }

        updateDevice(device);
    }

    public DeviceState getDeviceState(long deviceId) {
        return deviceStates.computeIfAbsent(deviceId, x -> new DeviceState());
    }

    public void setDeviceState(long deviceId, DeviceState deviceState) {
        deviceStates.put(deviceId, deviceState);
    }

    public Map<Event, Position> updateDeviceState(long deviceId) {
        DeviceState deviceState = getDeviceState(deviceId);
        Map<Event, Position> result = new HashMap<>();

        Map<Event, Position> event = Main.getInjector()
                .getInstance(MotionEventHandler.class).updateMotionState(deviceState);
        if (event != null) {
            result.putAll(event);
        }

        double speedLimit = AttributeUtil.lookup(cacheManager, Keys.EVENT_OVERSPEED_LIMIT, deviceId);
        event = Main.getInjector().getInstance(OverspeedEventHandler.class)
                .updateOverspeedState(deviceState, speedLimit);
        if (event != null) {
            result.putAll(event);
        }

        return result;
    }

    public synchronized void sendKeepalive() {
        for (Set<UpdateListener> userListeners : listeners.values()) {
            for (UpdateListener listener : userListeners) {
                listener.onKeepalive();
            }
        }
    }

    public synchronized void updateDevice(Device device) {
        for (User user : cacheManager.getDeviceObjects(device.getId(), User.class)) {
            if (listeners.containsKey(user.getId())) {
                for (UpdateListener listener : listeners.get(user.getId())) {
                    listener.onUpdateDevice(device);
                }
            }
        }
    }

    public synchronized void updatePosition(Position position) {
        long deviceId = position.getDeviceId();
        for (User user : cacheManager.getDeviceObjects(deviceId, User.class)) {
            if (listeners.containsKey(user.getId())) {
                for (UpdateListener listener : listeners.get(user.getId())) {
                    listener.onUpdatePosition(position);
                }
            }
        }
    }

    public synchronized void updateEvent(long userId, Event event) {
        if (listeners.containsKey(userId)) {
            for (UpdateListener listener : listeners.get(userId)) {
                listener.onUpdateEvent(event);
            }
        }
    }

    public interface UpdateListener {
        void onKeepalive();
        void onUpdateDevice(Device device);
        void onUpdatePosition(Position position);
        void onUpdateEvent(Event event);
    }

    public synchronized void addListener(long userId, UpdateListener listener) {
        if (!listeners.containsKey(userId)) {
            listeners.put(userId, new HashSet<>());
        }
        listeners.get(userId).add(listener);
    }

    public synchronized void removeListener(long userId, UpdateListener listener) {
        if (!listeners.containsKey(userId)) {
            listeners.put(userId, new HashSet<>());
        }
        listeners.get(userId).remove(listener);
    }

}
