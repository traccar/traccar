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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.model.BaseModel;
import org.traccar.model.User;

public class UsersManager extends SimpleObjectManager {

    private Map<String, Long> usersTokens;

    public UsersManager(DataManager dataManager) {
        super(dataManager, User.class);
    }

    private void putToken(User user) {
        if (usersTokens == null) {
            usersTokens = new ConcurrentHashMap<>();
        }
        if (user.getToken() != null) {
            usersTokens.put(user.getToken(), user.getId());
        }
    }

    @Override
    protected void addNewItem(BaseModel item) {
        super.addNewItem(item);
        putToken((User) item);
    }

    @Override
    protected void updateCachedItem(BaseModel item) {
        User user = (User) item;
        User cachedUser = (User) getById(item.getId());
        super.updateCachedItem(item);
        if (user.getToken() != null) {
            usersTokens.put(user.getToken(), user.getId());
        }
        if (cachedUser.getToken() != null && !cachedUser.getToken().equals(user.getToken())) {
            usersTokens.remove(cachedUser.getToken());
        }
    }

    @Override
    protected void removeCachedItem(long userId) {
        User cachedUser = (User) getById(userId);
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
        return getUserItems(userId);
    }

    public User getUserByToken(String token) {
        return (User) getById(usersTokens.get(token));
    }

}
