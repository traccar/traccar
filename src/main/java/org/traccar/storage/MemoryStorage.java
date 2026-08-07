/*
 * Copyright 2022 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.storage;

import org.traccar.helper.ReflectionCache;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.GroupedModel;
import org.traccar.model.Pair;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class MemoryStorage extends Storage {

    private final Map<Class<?>, Map<Long, Object>> objects = new ConcurrentHashMap<>();
    private final Map<Pair<Class<?>, Class<?>>, Set<Pair<Long, Long>>> permissions = new ConcurrentHashMap<>();

    private final AtomicLong increment = new AtomicLong();

    public MemoryStorage() {
        Server server = new Server();
        server.setId(1);
        server.setRegistration(true);
        objects.put(Server.class, Map.of(server.getId(), server));
    }

    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) {
        try (var objects = getObjectsStream(clazz, request)) {
            return objects.toList();
        }
    }

    @Override
    public <T> Stream<T> getObjectsStream(Class<T> clazz, Request request) {
        var stream = objects.computeIfAbsent(clazz, key -> new ConcurrentHashMap<>()).values().stream()
                .filter(object -> checkCondition(request.getCondition(), object));
        Order order = request.getOrder();
        if (order != null) {
            stream = stream.sorted((a, b) -> compareByOrder(a, b, order));
            if (order.getOffset() > 0) {
                stream = stream.skip(order.getOffset());
            }
            if (order.getLimit() > 0) {
                stream = stream.limit(order.getLimit());
            }
        }
        return stream.map(object -> (T) object);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareByOrder(Object a, Object b, Order order) {
        int comparison = ((Comparable) retrieveValue(a, order.getColumn()))
                .compareTo(retrieveValue(b, order.getColumn()));
        return order.getDescending() ? -comparison : comparison;
    }

    private boolean checkCondition(Condition genericCondition, Object object) {
        if (genericCondition == null) {
            return true;
        }
        return switch (genericCondition) {
            case Condition.Compare condition -> {
                Object value = retrieveValue(object, condition.getColumn());
                int result = ((Comparable) value).compareTo(condition.getValue());
                yield switch (condition.getOperator()) {
                    case "<" -> result < 0;
                    case "<=" -> result <= 0;
                    case ">" -> result > 0;
                    case ">=" -> result >= 0;
                    case "=" -> result == 0;
                    default -> throw new RuntimeException("Unsupported comparison condition");
                };
            }
            case Condition.Between condition -> {
                Object fromValue = retrieveValue(object, condition.getColumn());
                int fromResult = ((Comparable) fromValue).compareTo(condition.getFromValue());
                Object toValue = retrieveValue(object, condition.getColumn());
                int toResult = ((Comparable) toValue).compareTo(condition.getToValue());
                yield fromResult >= 0 && toResult <= 0;
            }
            case Condition.Binary condition -> switch (condition.getOperator()) {
                case "AND" -> checkCondition(condition.getFirst(), object)
                        && checkCondition(condition.getSecond(), object);
                case "OR" -> checkCondition(condition.getFirst(), object)
                        || checkCondition(condition.getSecond(), object);
                default -> false;
            };
            case Condition.Permission condition -> checkPermission(condition, object);
            case Condition.Contains condition -> {
                String needle = condition.getValue().toLowerCase(Locale.ROOT);
                yield condition.getColumns().stream().anyMatch(column -> {
                    Object value = retrieveValue(object, column);
                    return value != null && value.toString().toLowerCase(Locale.ROOT).contains(needle);
                });
            }
            case Condition.LatestPositions condition -> {
                long positionId = (Long) retrieveValue(object, "id");
                long positionDeviceId = (Long) retrieveValue(object, "deviceId");
                if (condition.getDeviceId() > 0 && positionDeviceId != condition.getDeviceId()) {
                    yield false;
                }
                yield objects.computeIfAbsent(Device.class, key -> new ConcurrentHashMap<>()).values().stream()
                        .anyMatch(device -> (Long) retrieveValue(device, "positionId") == positionId);
            }
            default -> false;
        };
    }

    private boolean checkPermission(Condition.Permission condition, Object object) {
        long objectId = (Long) retrieveValue(object, "id");
        Class<?> ownerClass = condition.getOwnerClass();
        Class<?> propertyClass = condition.getPropertyClass();
        boolean ownerFixed = condition.getOwnerId() > 0;
        long fixedId = ownerFixed ? condition.getOwnerId() : condition.getPropertyId();

        if (hasPermissionPair(ownerClass, propertyClass, ownerFixed, fixedId, objectId)) {
            return true;
        }

        if (condition.getIncludeGroups()) {
            Class<?> variableClass = ownerFixed ? propertyClass : ownerClass;
            if (GroupedModel.class.isAssignableFrom(variableClass) && object instanceof GroupedModel grouped) {
                Set<Long> ancestors = new HashSet<>();
                collectAncestorGroups(grouped, ancestors);
                Class<?> groupOwnerClass = ownerFixed ? ownerClass : Group.class;
                Class<?> groupPropertyClass = ownerFixed ? Group.class : propertyClass;
                for (long ancestorId : ancestors) {
                    if (hasPermissionPair(groupOwnerClass, groupPropertyClass, ownerFixed, fixedId, ancestorId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean hasPermissionPair(
            Class<?> ownerClass, Class<?> propertyClass, boolean ownerFixed, long fixedId, long variableId) {
        Set<Pair<Long, Long>> permissions = getPermissionsSet(ownerClass, propertyClass);
        return ownerFixed
                ? permissions.contains(new Pair<>(fixedId, variableId))
                : permissions.contains(new Pair<>(variableId, fixedId));
    }

    private void collectAncestorGroups(GroupedModel object, Set<Long> result) {
        long groupId = object.getGroupId();
        int depth = 0;
        while (groupId > 0 && depth++ < MAX_GROUP_DEPTH) {
            if (!result.add(groupId)) {
                break;
            }
            Object group = objects.computeIfAbsent(Group.class, key -> new ConcurrentHashMap<>()).get(groupId);
            if (group instanceof GroupedModel parent) {
                groupId = parent.getGroupId();
            } else {
                break;
            }
        }
    }

    private Object retrieveValue(Object object, String key) {
        MethodHandle handle = ReflectionCache.getProperties(object.getClass(), "get").get(key).handle();
        try {
            return handle.invokeExact(object);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> long addObject(T entity, Request request) {
        long id = increment.incrementAndGet();
        var items = objects.computeIfAbsent(entity.getClass(), key -> new ConcurrentHashMap<>());

        if (entity instanceof Position position) {
            var iterator = items.values().iterator();
            while (iterator.hasNext()) {
                Position item = (Position) iterator.next();
                if (item.getDeviceId() == position.getDeviceId()) {
                    if (item.getFixTime().after(position.getFixTime())) {
                        return id;
                    }
                    iterator.remove();
                    break;
                }
            }
        }

        items.put(id, entity);
        return id;
    }

    @Override
    public <T> void updateObject(T entity, Request request) {
        Collection<Object> items;
        if (request.getCondition() != null) {
            long id = (Long) ((Condition.Equals) request.getCondition()).getValue();
            Object item = objects.computeIfAbsent(entity.getClass(), key -> new ConcurrentHashMap<>()).get(id);
            items = item != null ? List.of(item) : List.of();
        } else {
            items = objects.computeIfAbsent(entity.getClass(), key -> new ConcurrentHashMap<>()).values();
        }
        var getters = ReflectionCache.getProperties(entity.getClass(), "get");
        var setters = ReflectionCache.getProperties(entity.getClass(), "set");
        for (String column : request.getColumns().getColumns(entity.getClass(), "get")) {
            MethodHandle setter = setters.get(column).handle();
            MethodHandle getter = getters.get(column).handle();
            try {
                Object value = getter.invokeExact(entity);
                for (Object object : items) {
                    setter.invokeExact(object, value);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void removeObject(Class<?> clazz, Request request) {
        long id = (Long) ((Condition.Equals) request.getCondition()).getValue();
        objects.computeIfAbsent(clazz, key -> new ConcurrentHashMap<>()).remove(id);
    }

    private Set<Pair<Long, Long>> getPermissionsSet(Class<?> ownerClass, Class<?> propertyClass) {
        return permissions.computeIfAbsent(new Pair<>(ownerClass, propertyClass), k -> ConcurrentHashMap.newKeySet());
    }

    @Override
    public List<Permission> getPermissions(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) {
        return getPermissionsSet(ownerClass, propertyClass).stream()
                .filter(pair -> ownerId == 0 || pair.first().equals(ownerId))
                .filter(pair -> propertyId == 0 || pair.second().equals(propertyId))
                .map(pair -> new Permission(ownerClass, pair.first(), propertyClass, pair.second()))
                .toList();
    }

    @Override
    public void addPermission(Permission permission) {
        getPermissionsSet(permission.getOwnerClass(), permission.getPropertyClass())
                .add(new Pair<>(permission.getOwnerId(), permission.getPropertyId()));
    }

    @Override
    public void removePermission(Permission permission) {
        getPermissionsSet(permission.getOwnerClass(), permission.getPropertyClass())
                .remove(new Pair<>(permission.getOwnerId(), permission.getPropertyId()));
    }

}
