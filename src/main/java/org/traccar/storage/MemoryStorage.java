package org.traccar.storage;

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
    public List<Permission> getPermissions(Class<?> ownerClass, Class<?> propertyClass) {
        return getPermissionsSet(ownerClass, propertyClass).stream()
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
