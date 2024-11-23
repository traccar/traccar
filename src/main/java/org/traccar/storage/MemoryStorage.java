/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.BaseModel;
import org.traccar.model.Pair;
import org.traccar.model.Permission;
import org.traccar.model.Server;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MemoryStorage extends Storage {

    private final Map<Class<?>, Map<Long, Object>> objects = new HashMap<>();
    private final Map<Pair<Class<?>, Class<?>>, Set<Pair<Long, Long>>> permissions = new HashMap<>();

    private final AtomicLong increment = new AtomicLong();

    public MemoryStorage() {
        Server server = new Server();
        server.setId(1);
        server.setRegistration(true);
        objects.put(Server.class, Map.of(server.getId(), server));
    }

    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) {
        return objects.computeIfAbsent(clazz, key -> new HashMap<>()).values().stream()
                .filter(object -> checkCondition(request.getCondition(), object))
                .map(object -> (T) object)
                .collect(Collectors.toList());
    }

    private boolean checkCondition(Condition genericCondition, Object object) {
        if (genericCondition == null) {
            return true;
        }

        if (genericCondition instanceof Condition.Compare condition) {

            Object value = retrieveValue(object, condition.getVariable());
            int result = ((Comparable) value).compareTo(condition.getValue());
            return switch (condition.getOperator()) {
                case "<" -> result < 0;
                case "<=" -> result <= 0;
                case ">" -> result > 0;
                case ">=" -> result >= 0;
                case "=" -> result == 0;
                default -> throw new RuntimeException("Unsupported comparison condition");
            };

        } else if (genericCondition instanceof Condition.Between condition) {

            Object fromValue = retrieveValue(object, condition.getFromVariable());
            int fromResult = ((Comparable) fromValue).compareTo(condition.getFromValue());
            Object toValue = retrieveValue(object, condition.getToVariable());
            int toResult = ((Comparable) toValue).compareTo(condition.getToValue());
            return fromResult >= 0 && toResult <= 0;

        } else if (genericCondition instanceof Condition.Binary condition) {

            if (condition.getOperator().equals("AND")) {
                return checkCondition(condition.getFirst(), object) && checkCondition(condition.getSecond(), object);
            } else if (condition.getOperator().equals("OR")) {
                return checkCondition(condition.getFirst(), object) || checkCondition(condition.getSecond(), object);
            }

        } else if (genericCondition instanceof Condition.Permission condition) {

            long id = (Long) retrieveValue(object, "id");
            return getPermissionsSet(condition.getOwnerClass(), condition.getPropertyClass()).stream()
                    .anyMatch(pair -> {
                        if (condition.getOwnerId() > 0) {
                            return pair.first() == condition.getOwnerId() && pair.second() == id;
                        } else {
                            return pair.first() == id && pair.second() == condition.getPropertyId();
                        }
                    });

        } else if (genericCondition instanceof Condition.LatestPositions) {

            return false;

        }

        return false;
    }

    private Object retrieveValue(Object object, String key) {
        try {
            Method method = object.getClass().getMethod(
                    "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1));
            return method.invoke(object);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> long addObject(T entity, Request request) {
        long id = increment.incrementAndGet();
        objects.computeIfAbsent(entity.getClass(), key -> new HashMap<>()).put(id, entity);
        return id;
    }

    @Override
    public <T> void updateObject(T entity, Request request) {
        Set<String> columns = new HashSet<>(request.getColumns().getColumns(entity.getClass(), "get"));
        Collection<Object> items;
        if (request.getCondition() != null) {
            long id = (Long) ((Condition.Equals) request.getCondition()).getValue();
            items = List.of(objects.computeIfAbsent(entity.getClass(), key -> new HashMap<>()).get(id));
        } else {
            items = objects.computeIfAbsent(entity.getClass(), key -> new HashMap<>()).values();
        }
        for (Method setter : entity.getClass().getMethods()) {
            if (setter.getName().startsWith("set") && setter.getParameterCount() == 1
                    && columns.contains(Introspector.decapitalize(setter.getName()))) {
                try {
                    Method getter = entity.getClass().getMethod(setter.getName().replaceFirst("set", "get"));
                    Object value = getter.invoke(entity);
                    for (Object object : items) {
                        setter.invoke(object, value);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void removeObject(Class<?> clazz, Request request) {
        long id = (Long) ((Condition.Equals) request.getCondition()).getValue();
        objects.computeIfAbsent(clazz, key -> new HashMap<>()).remove(id);
    }

    private Set<Pair<Long, Long>> getPermissionsSet(Class<?> ownerClass, Class<?> propertyClass) {
        return permissions.computeIfAbsent(new Pair<>(ownerClass, propertyClass), k -> new HashSet<>());
    }

    @Override
    public List<Permission> getPermissions(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) {
        return getPermissionsSet(ownerClass, propertyClass).stream()
                .filter(pair -> ownerId == 0 || pair.first().equals(ownerId))
                .filter(pair -> propertyId == 0 || pair.second().equals(propertyId))
                .map(pair -> new Permission(ownerClass, pair.first(), propertyClass, pair.second()))
                .collect(Collectors.toList());
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
