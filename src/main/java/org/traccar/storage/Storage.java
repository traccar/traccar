package org.traccar.storage;

import org.traccar.model.Permission;
import org.traccar.storage.query.Request;

import java.util.List;

public abstract class Storage {

    public abstract <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException;

    public abstract <T> long addObject(T entity, Request request) throws StorageException;

    public abstract <T> void updateObject(T entity, Request request) throws StorageException;

    public abstract void removeObject(Class<?> clazz, Request request) throws StorageException;

    public abstract List<Permission> getPermissions(
            Class<?> ownerClass, Class<?> propertyClass) throws StorageException;

    public abstract void addPermission(Permission permission) throws StorageException;

    public abstract void removePermission(Permission permission) throws StorageException;

    public <T> T getObject(Class<T> clazz, Request request) throws StorageException {
        var objects = getObjects(clazz, request);
        return objects.isEmpty() ? null : objects.get(0);
    }

}
