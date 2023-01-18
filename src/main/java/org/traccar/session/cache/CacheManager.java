/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.broadcast.BroadcastInterface;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.model.Attribute;
import org.traccar.model.BaseModel;
import org.traccar.model.Calendar;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.GroupedModel;
import org.traccar.model.Maintenance;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.ScheduledModel;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Singleton
public class CacheManager implements BroadcastInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private static final int GROUP_DEPTH_LIMIT = 3;
    private static final Collection<Class<? extends BaseModel>> CLASSES = Arrays.asList(
            Attribute.class, Driver.class, Geofence.class, Maintenance.class, Notification.class);

    private final Config config;
    private final Storage storage;
    private final BroadcastService broadcastService;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<CacheKey, CacheValue> deviceCache = new HashMap<>();
    private final Map<Long, Integer> deviceReferences = new HashMap<>();
    private final Map<Long, Map<Class<? extends BaseModel>, Set<Long>>> deviceLinks = new HashMap<>();
    private final Map<Long, Position> devicePositions = new HashMap<>();

    private Server server;
    private final Map<Long, List<User>> notificationUsers = new HashMap<>();

    @Inject
    public CacheManager(Config config, Storage storage, BroadcastService broadcastService) throws StorageException {
        this.config = config;
        this.storage = storage;
        this.broadcastService = broadcastService;
        invalidateServer();
        invalidateUsers();
        broadcastService.registerListener(this);
    }

    public Config getConfig() {
        return config;
    }

    public <T extends BaseModel> T getObject(Class<T> clazz, long id) {
        try {
            lock.readLock().lock();
            var cacheValue = deviceCache.get(new CacheKey(clazz, id));
            return cacheValue != null ? cacheValue.getValue() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T extends BaseModel> List<T> getDeviceObjects(long deviceId, Class<T> clazz) {
        try {
            lock.readLock().lock();
            var links = deviceLinks.get(deviceId);
            if (links != null) {
                return links.getOrDefault(clazz, new LinkedHashSet<>()).stream()
                        .map(id -> {
                            var cacheValue = deviceCache.get(new CacheKey(clazz, id));
                            return cacheValue != null ? cacheValue.<T>getValue() : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                LOGGER.warn("Device {} cache missing", deviceId);
                return Collections.emptyList();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public Position getPosition(long deviceId) {
        try {
            lock.readLock().lock();
            return devicePositions.get(deviceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Server getServer() {
        try {
            lock.readLock().lock();
            return server;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> getNotificationUsers(long notificationId, long deviceId) {
        try {
            lock.readLock().lock();
            var users = deviceLinks.get(deviceId).get(User.class).stream()
                    .collect(Collectors.toUnmodifiableSet());
            return notificationUsers.getOrDefault(notificationId, new LinkedList<>()).stream()
                    .filter(user -> users.contains(user.getId()))
                    .collect(Collectors.toUnmodifiableList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Driver findDriverByUniqueId(long deviceId, String driverUniqueId) {
        return getDeviceObjects(deviceId, Driver.class).stream()
                .filter(driver -> driver.getUniqueId().equals(driverUniqueId))
                .findFirst()
                .orElse(null);
    }

    public void addDevice(long deviceId) throws StorageException {
        try {
            lock.writeLock().lock();
            Integer references = deviceReferences.get(deviceId);
            if (references != null) {
                references += 1;
            } else {
                unsafeAddDevice(deviceId);
                references = 1;
            }
            deviceReferences.put(deviceId, references);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeDevice(long deviceId) {
        try {
            lock.writeLock().lock();
            Integer references = deviceReferences.get(deviceId);
            if (references != null) {
                references -= 1;
                if (references <= 0) {
                    unsafeRemoveDevice(deviceId);
                    deviceReferences.remove(deviceId);
                } else {
                    deviceReferences.put(deviceId, references);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updatePosition(Position position) {
        try {
            lock.writeLock().lock();
            if (deviceLinks.containsKey(position.getDeviceId())) {
                devicePositions.put(position.getDeviceId(), position);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void invalidateObject(boolean local, Class<? extends BaseModel> clazz, long id) {
        try {
            var object = storage.getObject(clazz, new Request(
                    new Columns.All(), new Condition.Equals("id", id)));
            if (object != null) {
                updateOrInvalidate(local, object);
            } else {
                invalidate(clazz, id);
            }
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends BaseModel> void updateOrInvalidate(boolean local, T object) throws StorageException {
        if (local) {
            broadcastService.invalidateObject(true, object.getClass(), object.getId());
        }

        if (object instanceof Server) {
            invalidateServer();
            return;
        }
        if (object instanceof User) {
            invalidateUsers();
            return;
        }

        boolean invalidate = false;
        var before = getObject(object.getClass(), object.getId());
        if (before == null) {
            return;
        } else if (object instanceof GroupedModel) {
            if (((GroupedModel) before).getGroupId() != ((GroupedModel) object).getGroupId()) {
                invalidate = true;
            }
        } else if (object instanceof ScheduledModel) {
            if (((ScheduledModel) before).getCalendarId() != ((ScheduledModel) object).getCalendarId()) {
                invalidate = true;
            }
        }
        if (invalidate) {
            invalidate(object.getClass(), object.getId());
        } else {
            try {
                lock.writeLock().lock();
                deviceCache.get(new CacheKey(object.getClass(), object.getId())).setValue(object);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public <T extends BaseModel> void invalidate(Class<T> clazz, long id) throws StorageException {
        invalidate(new CacheKey(clazz, id));
    }

    @Override
    public void invalidatePermission(
            boolean local,
            Class<? extends BaseModel> clazz1, long id1,
            Class<? extends BaseModel> clazz2, long id2) {
        if (local) {
            broadcastService.invalidatePermission(true, clazz1, id1, clazz2, id2);
        }

        try {
            invalidate(new CacheKey(clazz1, id1), new CacheKey(clazz2, id2));
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    private void invalidateServer() throws StorageException {
        server = storage.getObject(Server.class, new Request(new Columns.All()));
    }

    private void invalidateUsers() throws StorageException {
        notificationUsers.clear();
        Map<Long, User> users = new HashMap<>();
        storage.getObjects(User.class, new Request(new Columns.All()))
                .forEach(user -> users.put(user.getId(), user));
        storage.getPermissions(User.class, Notification.class).forEach(permission -> {
            long notificationId = permission.getPropertyId();
            var user = users.get(permission.getOwnerId());
            notificationUsers.computeIfAbsent(notificationId, k -> new LinkedList<>()).add(user);
        });
    }

    private void addObject(long deviceId, BaseModel object) {
        deviceCache.computeIfAbsent(new CacheKey(object), k -> new CacheValue(object)).retain(deviceId);
    }

    private void unsafeAddDevice(long deviceId) throws StorageException {
        Map<Class<? extends BaseModel>, Set<Long>> links = new HashMap<>();

        Device device = storage.getObject(Device.class, new Request(
                new Columns.All(), new Condition.Equals("id", deviceId)));
        if (device != null) {
            addObject(deviceId, device);

            int groupDepth = 0;
            long groupId = device.getGroupId();
            while (groupDepth < GROUP_DEPTH_LIMIT && groupId > 0) {
                Group group = storage.getObject(Group.class, new Request(
                        new Columns.All(), new Condition.Equals("id", groupId)));
                links.computeIfAbsent(Group.class, k -> new LinkedHashSet<>()).add(group.getId());
                addObject(deviceId, group);
                groupId = group.getGroupId();
                groupDepth += 1;
            }

            for (Class<? extends BaseModel> clazz : CLASSES) {
                var objects = storage.getObjects(clazz, new Request(
                        new Columns.All(), new Condition.Permission(Device.class, deviceId, clazz)));
                links.put(clazz, objects.stream().map(BaseModel::getId).collect(Collectors.toSet()));
                for (var object : objects) {
                    addObject(deviceId, object);
                    if (object instanceof ScheduledModel) {
                        var scheduled = (ScheduledModel) object;
                        if (scheduled.getCalendarId() > 0) {
                            var calendar = storage.getObject(Calendar.class, new Request(
                                    new Columns.All(), new Condition.Equals("id", scheduled.getCalendarId())));
                            links.computeIfAbsent(Notification.class, k -> new LinkedHashSet<>())
                                    .add(calendar.getId());
                            addObject(deviceId, calendar);
                        }
                    }
                }
            }

            var users = storage.getObjects(User.class, new Request(
                    new Columns.All(), new Condition.Permission(User.class, Device.class, deviceId)));
            links.put(User.class, users.stream().map(BaseModel::getId).collect(Collectors.toSet()));
            for (var user : users) {
                addObject(deviceId, user);
                var notifications = storage.getObjects(Notification.class, new Request(
                        new Columns.All(),
                        new Condition.Permission(User.class, user.getId(), Notification.class))).stream()
                        .filter(Notification::getAlways)
                        .collect(Collectors.toList());
                for (var notification : notifications) {
                    links.computeIfAbsent(Notification.class, k -> new LinkedHashSet<>())
                            .add(notification.getId());
                    addObject(deviceId, notification);
                    if (notification.getCalendarId() > 0) {
                        var calendar = storage.getObject(Calendar.class, new Request(
                                new Columns.All(), new Condition.Equals("id", notification.getCalendarId())));
                        links.computeIfAbsent(Notification.class, k -> new LinkedHashSet<>())
                                .add(calendar.getId());
                        addObject(deviceId, calendar);
                    }
                }
            }

            deviceLinks.put(deviceId, links);

            if (device.getPositionId() > 0) {
                devicePositions.put(deviceId, storage.getObject(Position.class, new Request(
                        new Columns.All(), new Condition.Equals("id", device.getPositionId()))));
            }
        }
    }

    private void unsafeRemoveDevice(long deviceId) {
        deviceCache.remove(new CacheKey(Device.class, deviceId));
        deviceLinks.remove(deviceId).forEach((clazz, ids) -> ids.forEach(id -> {
            var key = new CacheKey(clazz, id);
            deviceCache.computeIfPresent(key, (k, value) -> {
                value.release(deviceId);
                return value.getReferences().size() > 0 ? value : null;
            });
        }));
        devicePositions.remove(deviceId);
    }

    private void invalidate(CacheKey... keys) throws StorageException {
        try {
            lock.writeLock().lock();
            unsafeInvalidate(keys);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void unsafeInvalidate(CacheKey[] keys) throws StorageException {
        boolean invalidateServer = false;
        boolean invalidateUsers = false;
        Set<Long> linkedDevices = new HashSet<>();
        for (var key : keys) {
            if (key.classIs(Server.class)) {
                invalidateServer = true;
            } else {
                if (key.classIs(User.class) || key.classIs(Notification.class)) {
                    invalidateUsers = true;
                }
                deviceCache.computeIfPresent(key, (k, value) -> {
                    linkedDevices.addAll(value.getReferences());
                    return value;
                });
            }
        }
        for (long deviceId : linkedDevices) {
            unsafeRemoveDevice(deviceId);
            unsafeAddDevice(deviceId);
        }
        if (invalidateServer) {
            invalidateServer();
        }
        if (invalidateUsers) {
            invalidateUsers();
        }
    }

}
