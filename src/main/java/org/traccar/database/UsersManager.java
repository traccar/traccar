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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.model.User;
import org.traccar.storage.StorageException;

public class UsersManager extends SimpleObjectManager<User> {

    private Map<String, User> usersTokens;

    public UsersManager(DataManager dataManager) {
        super(dataManager, User.class);
        if (usersTokens == null) {
            usersTokens = new ConcurrentHashMap<>();
        }
    }

    private void putToken(User user) {
        if (usersTokens == null) {
            usersTokens = new ConcurrentHashMap<>();
        }
        if (user.getToken() != null) {
            usersTokens.put(user.getToken(), user);
        }
    }

    @Override
    protected void addNewItem(User user) {
        super.addNewItem(user);
        putToken(user);
    }

    @Override
    protected void updateCachedItem(User user) {
        User cachedUser = getById(user.getId());
        super.updateCachedItem(user);
        putToken(user);
        if (cachedUser.getToken() != null && !cachedUser.getToken().equals(user.getToken())) {
            usersTokens.remove(cachedUser.getToken());
        }
    }

    @Override
    public void updateItem(User user) throws StorageException {
        if (user.getHashedPassword() != null) {
            getDataManager().updateUserPassword(user);
        }
        super.updateItem(user);
    }

    @Override
    protected void removeCachedItem(long userId) {
        User cachedUser = getById(userId);
        if (cachedUser != null) {
            String userToken = cachedUser.getToken();
            super.removeCachedItem(userId);
            if (userToken != null) {
                usersTokens.remove(userToken);
            }
        }
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = getUserItems(userId);
        result.add(userId);
        return result;
    }

    public User getUserByToken(String token) {
        return usersTokens.get(token);
    }

}
