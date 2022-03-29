/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.BaseModel;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

public abstract class SimpleObjectManager<T extends BaseModel> extends BaseObjectManager<T>
        implements ManagableObjects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleObjectManager.class);

    private Map<Long, Set<Long>> userItems;

    protected SimpleObjectManager(DataManager dataManager, Class<T> baseClass) {
        super(dataManager, baseClass);
    }

    @Override
    public final Set<Long> getUserItems(long userId) {
        try {
            readLock();
            Set<Long> result = userItems.get(userId);
            if (result != null) {
                return new HashSet<>(result);
            } else {
                return new HashSet<>();
            }
        } finally {
            readUnlock();
        }
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = getUserItems(userId);
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }

    public final boolean checkItemPermission(long userId, long itemId) {
        return getUserItems(userId).contains(itemId);
    }

    @Override
    public void refreshItems() {
        super.refreshItems();
        refreshUserItems();
    }

    public final void refreshUserItems() {
        if (getDataManager() != null) {
            try {
                writeLock();
                userItems = new ConcurrentHashMap<>();
                for (Permission permission : getDataManager().getPermissions(User.class, getBaseClass())) {
                    Set<Long> items = userItems.computeIfAbsent(permission.getOwnerId(), key -> new HashSet<>());
                    items.add(permission.getPropertyId());
                }
            } catch (StorageException | ClassNotFoundException error) {
                LOGGER.warn("Error getting permissions", error);
            } finally {
                writeUnlock();
            }
        }
    }

    @Override
    public void removeItem(long itemId) throws StorageException {
        super.removeItem(itemId);
        refreshUserItems();
    }

}
