package org.traccar.storage;

import org.traccar.storage.query.Request;

import java.util.List;

public abstract class Storage {

    abstract <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException;

    abstract <T> long addObject(T entity, Request request) throws StorageException;

    abstract <T> void updateObject(T entity, Request request) throws StorageException;

    abstract void removeObject(Class<?> clazz, Request request) throws StorageException;

    <T> T getObject(Class<T> clazz, Request request) throws StorageException {
        var objects = getObjects(clazz, request);
        if (objects.isEmpty()) {
            throw new StorageException("No objects found");
        }
        return objects.get(0);
    }

}
