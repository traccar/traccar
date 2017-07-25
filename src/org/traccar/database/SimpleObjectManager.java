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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.BaseModel;
import org.traccar.model.Permission;
import org.traccar.model.User;

public abstract class SimpleObjectManager {

    private final DataManager dataManager;

    private Map<Long, BaseModel> items;
    private final Map<Long, Set<Long>> userItems = new ConcurrentHashMap<>();

    private Class<? extends BaseModel> baseClass;
    private String baseClassIdName;

    protected SimpleObjectManager(DataManager dataManager, Class<? extends BaseModel> baseClass) {
        this.dataManager = dataManager;
        this.baseClass = baseClass;
        baseClassIdName = DataManager.makeNameId(baseClass);
        refreshItems();
    }

    protected final DataManager getDataManager() {
        return dataManager;
    }

    protected final Class<? extends BaseModel> getBaseClass() {
        return baseClass;
    }

    protected final String getBaseClassIdName() {
        return baseClassIdName;
    }

    public final BaseModel getById(long itemId) {
        return items.get(itemId);
    }

    protected final void clearItems() {
        items.clear();
    }

    public final Set<Long> getUserItems(long userId) {
        if (!userItems.containsKey(userId)) {
            userItems.put(userId, new HashSet<Long>());
        }
        return userItems.get(userId);
    }

    protected final void clearUserItems() {
        userItems.clear();
    }

    public final boolean checkItemPermission(long userId, long itemId) {
        return getUserItems(userId).contains(itemId);
    }

    public void refreshItems() {
        if (dataManager != null) {
            try {
                Collection<? extends BaseModel> databaseItems = dataManager.getObjects(baseClass);
                if (items == null) {
                    items = new ConcurrentHashMap<>(databaseItems.size());
                }
                Set<Long> databaseItemIds = new HashSet<>();
                for (BaseModel item : databaseItems) {
                    databaseItemIds.add(item.getId());
                    if (items.containsKey(item.getId())) {
                        updateCachedItem(item);
                    } else {
                        addNewItem(item);
                    }
                }
                for (Long cachedItemId : items.keySet()) {
                    if (!databaseItemIds.contains(cachedItemId)) {
                        removeCachedItem(cachedItemId);
                    }
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserItems();
    }

    public final void refreshUserItems() {
        if (dataManager != null) {
            try {
                clearUserItems();
                for (Permission permission : dataManager.getPermissions(User.class, baseClass)) {
                    getUserItems(permission.getOwnerId()).add(permission.getPropertyId());
                }
            } catch (SQLException | ClassNotFoundException error) {
                Log.warning(error);
            }
        }
    }

    protected void addNewItem(BaseModel item) {
        items.put(item.getId(), item);
    }

    public void addItem(BaseModel item) throws SQLException {
        dataManager.addObject(item);
        addNewItem(item);
    }

    protected void updateCachedItem(BaseModel item) {
        items.put(item.getId(), item);
    }

    public void updateItem(BaseModel item) throws SQLException {
        dataManager.updateObject(item);
        updateCachedItem(item);
    }

    protected void removeCachedItem(long itemId) {
        items.remove(itemId);
    }

    public void removeItem(long itemId) throws SQLException {
        BaseModel item = getById(itemId);
        if (item != null) {
            dataManager.removeObject(baseClass, itemId);
            removeCachedItem(itemId);
        }
        refreshUserItems();
    }

    public final <T> Collection<T> getItems(Class<T> clazz, Set<Long> itemIds) {
        Collection<T> result = new LinkedList<>();
        for (long itemId : itemIds) {
            result.add((T) getById(itemId));
        }
        return result;
    }

    public final Set<Long> getAllItems() {
        return items.keySet();
    }

    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>();
        result.addAll(getUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getManagedItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }

}
