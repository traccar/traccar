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

import java.util.HashSet;
import java.util.Set;

import org.traccar.Context;
import org.traccar.model.User;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

public class UsersManager extends SimpleObjectManager<User> {

    private Cache<String, User> usersTokens;

    public UsersManager(DataManager dataManager) {
        super(dataManager, User.class);
    }

    public Cache<String, User> getUsersTokensCache() {
        if (usersTokens == null) {
            usersTokens = Context.getCacheManager().createCache(
                    this.getClass().getSimpleName() + "UsersTokens", new MutableConfiguration<>());
        }
        return usersTokens;
    }

    @Override
    protected void addNewItem(User user) {
        super.addNewItem(user);
        if (user.getToken() != null) {
            getUsersTokensCache().put(user.getToken(), user);
        }
    }

    @Override
    protected void updateCachedItem(User user) {
        User cachedUser = getById(user.getId());
        super.updateCachedItem(user);
        if (user.getToken() != null) {
            getUsersTokensCache().put(user.getToken(), user);
            if (!cachedUser.getToken().equals(user.getToken())) {
                getUsersTokensCache().remove(cachedUser.getToken());
            }
        }
    }

    @Override
    protected void removeCachedItem(long userId) {
        User cachedUser = getById(userId);
        if (cachedUser != null) {
            String userToken = cachedUser.getToken();
            super.removeCachedItem(userId);
            if (userToken != null) {
                getUsersTokensCache().remove(userToken);
            }
        }
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>();
        result.addAll(getUserItems(userId));
        result.add(userId);
        return result;
    }

    public User getUserByToken(String token) {
        return getUsersTokensCache().get(token);
    }

}
