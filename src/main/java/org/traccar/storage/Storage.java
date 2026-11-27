/*
 * Copyright 2022 - 2025 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.Permission;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class Storage {

    public static final int MAX_GROUP_DEPTH = 3;

    public abstract <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException;

    public abstract <T> Stream<T> getObjectsStream(Class<T> clazz, Request request) throws StorageException;

    public abstract <T> long addObject(T entity, Request request) throws StorageException;

    public <T> List<Long> addObjects(List<T> entities, Request request) throws StorageException {
        List<Long> ids = new ArrayList<>(entities.size());
        for (T entity : entities) {
            ids.add(addObject(entity, request));
        }
        return ids;
    }

    public abstract <T> void updateObject(T entity, Request request) throws StorageException;

    public <T extends BaseModel> void updateObjects(List<T> entities, Columns columns) throws StorageException {
        for (T entity : entities) {
            updateObject(entity, new Request(columns, new Condition.Equals("id", entity.getId())));
        }
    }

    public abstract void removeObject(Class<?> clazz, Request request) throws StorageException;

    public abstract List<Permission> getPermissions(List<Permission> filters) throws StorageException;

    public abstract void addPermission(Permission permission) throws StorageException;

    public abstract void removePermission(Permission permission) throws StorageException;

    public <T> T getObject(Class<T> clazz, Request request) throws StorageException {
        try (var objects = getObjectsStream(clazz, request)) {
            return objects.findFirst().orElse(null);
        }
    }

}
