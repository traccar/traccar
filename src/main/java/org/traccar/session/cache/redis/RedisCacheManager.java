/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.session.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.traccar.broadcast.BroadcastInterface;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.model.BaseModel;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Maintenance;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.model.Group;
import org.traccar.model.Calendar;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.GroupedModel;
import org.traccar.model.Permission;
import org.traccar.model.Schedulable;
import org.traccar.model.ObjectOperation;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;
import java.util.stream.Collectors;

public class RedisCacheManager implements BroadcastInterface, CacheManager {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RedisCacheManager.class);

    private final transient JedisPool jedisPool;
    private final transient RedisCacheGraph graph;
    protected final transient Storage storage;

    private static final Set<Class<? extends BaseModel>> GROUPED_CLASSES =
            Set.of(Attribute.class, Driver.class, Geofence.class, Maintenance.class, Notification.class);
    private final ObjectMapper objectMapper;
    private final BroadcastService broadcastService;
    private Server server;
    private final transient Config config;
    private static final String DEVICE_POSITIONS_KEY = "devicePositions";
    private static final String REFERENCES_SUFFIX = ":references";
    private static final String DEVICE_KEY_PREFIX = "device:";

    public RedisCacheManager(Config config, Storage storage, BroadcastService broadcastService,
                             ObjectMapper objectMapper, RedisCacheGraph graph, JedisPool jedisPool)
            throws StorageException {
        this.objectMapper = objectMapper;
        this.jedisPool = jedisPool;
        this.graph = graph;
        this.storage = storage;
        this.broadcastService = broadcastService;
        server = storage.getObject(Server.class, new Request(new Columns.All()));
        broadcastService.registerListener(this);
        this.config = config;
    }

    @Override
    public <T extends BaseModel> T getObject(Class<T> clazz, long id) {
        return graph.getObject(clazz, id);
    }

    @Override
    public <T extends BaseModel> Set<T> getDeviceObjects(long deviceId, Class<T> clazz) {
        return graph.getObjects(Device.class, deviceId, clazz, Set.of(Group.class), true)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Position getPosition(long deviceId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String data = jedis.hget(DEVICE_POSITIONS_KEY, String.valueOf(deviceId));
            if (data != null) {
                return objectMapper.readValue(data, Position.class);
            } else {
                Position position = storage.getObject(Position.class, new Request(
                        new Columns.All(), new Condition.Equals("deviceId", deviceId)));
                if (position != null) {
                    jedis.hset(DEVICE_POSITIONS_KEY, String.valueOf(deviceId),
                            objectMapper.writeValueAsString(position));
                }
                return position;
            }
        } catch (StorageException e) {
            throw new RuntimeException("Storage error", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updatePosition(Position position) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(DEVICE_POSITIONS_KEY,
                    String.valueOf(position.getDeviceId()), objectMapper.writeValueAsString(position));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addDevice(long deviceId, Object key) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String deviceKey = DEVICE_KEY_PREFIX + deviceId;
            String referencesKey = deviceKey + REFERENCES_SUFFIX;

            // Check if the device is already in the cache
            if (!jedis.exists(deviceKey)) {
                Device device = storage.getObject(Device.class, new Request(
                        new Columns.All(), new Condition.Equals("id", deviceId)));
                graph.addObject(device);
                initializeCache(device);
                if (device.getPositionId() > 0) {
                    Position position = storage.getObject(Position.class, new Request(
                            new Columns.All(), new Condition.Equals("id", device.getPositionId())));
                    jedis.hset(deviceKey, "position", objectMapper.writeValueAsString(position));
                }
            }
            // Add the reference key to the Redis set
            jedis.sadd(referencesKey, key.toString());
            Long referencesSize = jedis.scard(referencesKey);
            LOGGER.debug("Cache add device {} references {} key {}", deviceId, referencesSize, key);
        }
    }

    @Override
    public void removeDevice(long deviceId, Object key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String deviceKey = DEVICE_KEY_PREFIX + deviceId;
            String referencesKey = deviceKey + REFERENCES_SUFFIX;

            // Remove the reference key from the Redis set
            jedis.srem(referencesKey, key.toString());
            long referencesSize = jedis.scard(referencesKey);

            if (referencesSize == 0) {
                graph.removeObject(Device.class, deviceId);
                jedis.del(deviceKey);
                jedis.del(referencesKey);
            }
            LOGGER.debug("Cache remove device {} references {} key {}", deviceId, referencesSize, key);
        }
    }

    private void initializeCache(BaseModel object) throws Exception {
        if (object instanceof User) {
            for (Permission permission : storage.getPermissions(User.class, Notification.class)) {
                if (permission.getOwnerId() == object.getId()) {
                    invalidatePermission(
                            permission.getOwnerClass(), permission.getOwnerId(),
                            permission.getPropertyClass(), permission.getPropertyId(), true);
                }
            }
        } else {
            if (object instanceof GroupedModel groupedModel) {
                long groupId = groupedModel.getGroupId();
                if (groupId > 0) {
                    invalidatePermission(object.getClass(), object.getId(), Group.class, groupId, true);
                }

                for (Permission permission : storage.getPermissions(User.class, object.getClass())) {
                    if (permission.getPropertyId() == object.getId()) {
                        invalidatePermission(
                                object.getClass(), object.getId(), User.class, permission.getOwnerId(), true);
                    }
                }

                for (Class<? extends BaseModel> clazz : GROUPED_CLASSES) {
                    for (Permission permission : storage.getPermissions(object.getClass(), clazz)) {
                        if (permission.getOwnerId() == object.getId()) {
                            invalidatePermission(
                                    object.getClass(), object.getId(), clazz, permission.getPropertyId(), true);
                        }
                    }
                }
            }

            if (object instanceof Schedulable schedulable) {
                long calendarId = schedulable.getCalendarId();
                if (calendarId > 0) {
                    invalidatePermission(object.getClass(), object.getId(), Calendar.class, calendarId, true);
                }
            }
        }
    }


    @Override
    public <T extends BaseModel> void invalidateObject(
            boolean local, Class<T> clazz, long id, ObjectOperation operation) throws Exception {
        if (local) {
            broadcastService.invalidateObject(true, clazz, id, operation);
        }
        if (operation == ObjectOperation.DELETE) {
            graph.removeObject(clazz, id);
            return;
        }
        if (operation != ObjectOperation.UPDATE) {
            return;
        }
        if (clazz.equals(Server.class)) {
            server = storage.getObject(Server.class, new Request(new Columns.All()));
            graph.updateObject(server);
            return;
        }
        var after = storage.getObject(clazz, new Request(new Columns.All(), new Condition.Equals("id", id)));
        if (after == null) {
            return;
        }
        var before = getObject(after.getClass(), after.getId());
        if (before == null) {
            return;
        }
        if (after instanceof GroupedModel) {
            long beforeGroupId = ((GroupedModel) before).getGroupId();
            long afterGroupId = ((GroupedModel) after).getGroupId();
            if (beforeGroupId != afterGroupId) {
                if (beforeGroupId > 0) {
                    invalidatePermission(clazz, id, Group.class, beforeGroupId, false);
                }
                if (afterGroupId > 0) {
                    invalidatePermission(clazz, id, Group.class, afterGroupId, true);
                }
            }
        } else if (after instanceof Schedulable) {
            long beforeCalendarId = ((Schedulable) before).getCalendarId();
            long afterCalendarId = ((Schedulable) after).getCalendarId();
            if (beforeCalendarId != afterCalendarId) {
                if (beforeCalendarId > 0) {
                    invalidatePermission(clazz, id, Calendar.class, beforeCalendarId, false);
                }
                if (afterCalendarId > 0) {
                    invalidatePermission(clazz, id, Calendar.class, afterCalendarId, true);
                }
            }
        }
        graph.updateObject(after);
    }

    @Override
    public <T1 extends BaseModel, T2 extends BaseModel> void invalidatePermission(
            boolean local, Class<T1> clazz1, long id1, Class<T2> clazz2, long id2, boolean link) throws Exception {
        if (local) {
            broadcastService.invalidatePermission(true, clazz1, id1, clazz2, id2, link);
        }

        if (clazz1.equals(User.class) && GroupedModel.class.isAssignableFrom(clazz2)) {
            invalidatePermission(clazz2, id2, clazz1, id1, link);
        } else {
            invalidatePermission(clazz1, id1, clazz2, id2, link);
        }
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Server getServer() {
        return server;
    }

    private <T1 extends BaseModel, T2 extends BaseModel> void invalidatePermission(
            Class<T1> fromClass, long fromId, Class<T2> toClass, long toId, boolean link) throws Exception {

        boolean groupLink = GroupedModel.class.isAssignableFrom(fromClass) && toClass.equals(Group.class);
        boolean calendarLink = Schedulable.class.isAssignableFrom(fromClass) && toClass.equals(Calendar.class);
        boolean userLink = fromClass.equals(User.class) && toClass.equals(Notification.class);

        boolean groupedLinks = GroupedModel.class.isAssignableFrom(fromClass)
                && (GROUPED_CLASSES.contains(toClass) || toClass.equals(User.class));

        if (!groupLink && !calendarLink && !userLink && !groupedLinks) {
            return;
        }
        if (link) {
            BaseModel object = storage.getObject(toClass, new Request(
                    new Columns.All(), new Condition.Equals("id", toId)));
            if (!graph.addLink(fromClass, fromId, object)) {
                initializeCache(object);
            }
        } else {
            graph.removeLink(fromClass, fromId, toClass, toId);
        }
    }

    @Override
    public Set<User> getNotificationUsers(long notificationId, long deviceId) {
        try {
            Set<User> deviceUsers = getDeviceObjects(deviceId, User.class);
            return graph.getObjects(Notification.class, notificationId, User.class, Set.of(), false)
                    .filter(user -> deviceUsers.stream().anyMatch(deviceUser -> deviceUser.compare(user)))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Notification> getDeviceNotifications(long deviceId) {
        try {
            var direct = graph.getObjects(Device.class, deviceId, Notification.class, Set.of(Group.class), true)
                    .map(BaseModel::getId)
                    .collect(Collectors.toUnmodifiableSet());
            return graph.getObjects(Device.class, deviceId, Notification.class,
                            Set.of(Group.class, User.class), true)
                    .filter(notification -> notification.getAlways() || direct.contains(notification.getId()))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
