/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.ChildProfile;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class ChildProfileManager {

    private final Storage storage;

    @Inject
    public ChildProfileManager(Storage storage) {
        this.storage = storage;
    }

    public ChildProfile addProfile(ChildProfile profile) throws StorageException {
        profile.setId(storage.addObject(profile, new Request(new Columns.Exclude("id"))));
        return profile;
    }

    public void updateProfile(ChildProfile profile) throws StorageException {
        storage.updateObject(profile, new Request(
                new Columns.Exclude("id"), new Condition.Equals("id", profile.getId())));
    }

    public ChildProfile getProfile(long id) throws StorageException {
        return storage.getObject(ChildProfile.class, new Request(
                new Columns.All(), new Condition.Equals("id", id)));
    }

    public List<ChildProfile> getProfilesByDevice(long deviceId) throws StorageException {
        return storage.getObjects(ChildProfile.class, new Request(
                new Columns.All(), new Condition.Equals("deviceId", deviceId)));
    }

    public void deleteProfile(long id) throws StorageException {
        storage.removeObject(ChildProfile.class, new Request(new Condition.Equals("id", id)));
    }
}
