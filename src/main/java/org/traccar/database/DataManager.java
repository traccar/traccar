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
import org.traccar.model.Device;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Limit;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

public class DataManager {

    private final Storage storage;

    @Inject
    public DataManager(Storage storage) throws Exception {
        this.storage = storage;
    }

    public void updateDeviceStatus(Device device) throws StorageException {
        storage.updateObject(device, new Request(
                new Columns.Include("lastUpdate"),
                new Condition.Equals("id", "id")));
    }

    public Position getPrecedingPosition(long deviceId, Date date) throws StorageException {
        return storage.getObject(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", "deviceId", deviceId),
                        new Condition.Compare("fixTime", "<=", "time", date)),
                new Order(true, "fixTime"),
                new Limit(1)));
    }

    public void updateLatestPosition(Position position) throws StorageException {
        Device device = new Device();
        device.setId(position.getDeviceId());
        device.setPositionId(position.getId());
        storage.updateObject(device, new Request(
                new Columns.Include("positionId"),
                new Condition.Equals("id", "id")));
    }

    public Collection<Position> getLatestPositions() throws StorageException {
        return storage.getObjects(Position.class, new Request(
                new Columns.All(),
                new Condition.LatestPositions()));
    }

    public Server getServer() throws StorageException {
        return storage.getObject(Server.class, new Request(new Columns.All()));
    }

    public Collection<Permission> getPermissions(Class<? extends BaseModel> owner, Class<? extends BaseModel> property)
            throws StorageException, ClassNotFoundException {
        return storage.getPermissions(owner, property);
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
