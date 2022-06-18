/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import org.traccar.model.BaseModel;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import java.util.Collection;

public class DataManager {

    private final Storage storage;

    @Inject
    public DataManager(Storage storage) {
        this.storage = storage;
    }

    public Server getServer() throws StorageException {
        return storage.getObject(Server.class, new Request(new Columns.All()));
    }

    public <T extends BaseModel> Collection<T> getObjects(Class<T> clazz) throws StorageException {
        return storage.getObjects(clazz, new Request(new Columns.All()));
    }

    public void addObject(BaseModel entity) throws StorageException {
        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));
    }

    public void updateObject(BaseModel entity) throws StorageException {
        storage.updateObject(entity, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", "id")));
    }

    public void removeObject(Class<? extends BaseModel> clazz, long entityId) throws StorageException {
        storage.removeObject(clazz, new Request(new Condition.Equals("id", "id", entityId)));
    }

}
