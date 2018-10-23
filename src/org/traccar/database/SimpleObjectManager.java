/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.BaseModel;
import org.traccar.model.Permission;
import org.traccar.model.User;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

public abstract class SimpleObjectManager<T extends BaseModel> extends BaseObjectManager<T>
        implements ManagableObjects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleObjectManager.class);

    private Cache<Long, Set<Long>> userItems;

    protected SimpleObjectManager(DataManager dataManager, Class<T> baseClass) {
        super(dataManager, baseClass);
    }

    private synchronized Cache<Long, Set<Long>> getUserItemsCache() {
        if (userItems == null) {
            userItems = Context.getCacheManager().createCache(
                    this.getClass().getSimpleName() + "UserItems", new MutableConfiguration<>());
        }
        return userItems;
    }

    @Override
    public final Set<Long> getUserItems(long userId) {
        if (!getUserItemsCache().containsKey(userId)) {
            return Collections.emptySet();
        }
        return getUserItemsCache().get(userId);
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>(getUserItems(userId));
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

    protected final void updateCache(Cache<Long, Set<Long>> cache, Map<Long, Set<Long>> data) {
        for (Cache.Entry<Long, Set<Long>> entry : cache) {
            if (!data.containsKey(entry.getKey())) {
                cache.remove(entry.getKey());
            }
        }
        for (Map.Entry<Long, Set<Long>> entry : data.entrySet()) {
            if (!entry.getValue().equals(cache.get(entry.getKey()))) {
                cache.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public final void refreshUserItems() {
        if (getDataManager() != null) {
            try {
                Map<Long, Set<Long>> updatedUserItems = new HashMap<>();
                for (Permission permission : getDataManager().getPermissions(User.class, getBaseClass())) {
                    if (!updatedUserItems.containsKey(permission.getOwnerId())) {
                        updatedUserItems.put(permission.getOwnerId(), new HashSet<>());
                    }
                    updatedUserItems.get(permission.getOwnerId()).add(permission.getPropertyId());
                }
                updateCache(getUserItemsCache(), updatedUserItems);
            } catch (SQLException | ClassNotFoundException error) {
                LOGGER.warn("Error getting permissions", error);
            }
        }
    }

    @Override
    public void removeItem(long itemId) throws SQLException {
        super.removeItem(itemId);
        refreshUserItems();
    }

}
