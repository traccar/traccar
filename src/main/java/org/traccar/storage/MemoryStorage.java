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
package org.traccar.storage;

import org.traccar.model.BaseModel;
import org.traccar.model.Pair;
import org.traccar.model.Permission;
import org.traccar.storage.query.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MemoryStorage extends Storage {

    private final Map<Pair<Class<?>, Class<?>>, Set<Pair<Long, Long>>> permissions = new HashMap<>();

    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) {
        return null;
    }

    @Override
    public <T> long addObject(T entity, Request request) {
        return 0;
    }

    @Override
    public <T> void updateObject(T entity, Request request) {
    }

    @Override
    public void removeObject(Class<?> clazz, Request request) {
    }

    private Set<Pair<Long, Long>> getPermissionsSet(Class<?> ownerClass, Class<?> propertyClass) {
        return permissions.computeIfAbsent(new Pair<>(ownerClass, propertyClass), k -> new HashSet<>());
    }

    @Override
    public List<Permission> getPermissions(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) {
        return getPermissionsSet(ownerClass, propertyClass).stream()
                .filter(pair -> ownerId == 0 || pair.getFirst().equals(ownerId))
                .filter(pair -> propertyId == 0 || pair.getSecond().equals(propertyId))
                .map(pair -> new Permission(ownerClass, pair.getFirst(), propertyClass, pair.getSecond()))
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
