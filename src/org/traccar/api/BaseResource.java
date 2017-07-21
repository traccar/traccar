/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.core.SecurityContext;

import org.traccar.Context;
import org.traccar.database.DataManager;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.User;

public class BaseResource {

    @javax.ws.rs.core.Context
    private SecurityContext securityContext;

    protected long getUserId() {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        if (principal != null) {
            return principal.getUserId();
        }
        return 0;
    }

    protected void checkAndLinkPermission(LinkedHashMap<String, Long> entity, boolean link)
            throws SQLException, ClassNotFoundException {
        Iterator<String> iterator = entity.keySet().iterator();
        String owner = iterator.next();
        Class<?> ownerClass = DataManager.getClassByName(owner);
        String property = iterator.next();
        Class<?> propertyClass = DataManager.getClassByName(property);

        long ownerId = entity.get(owner);
        long propertyId = entity.get(property);

        if (!link && ownerClass.equals(User.class)
                && propertyClass.equals(Device.class)) {
            if (getUserId() != ownerId) {
                Context.getPermissionsManager().checkUser(getUserId(), ownerId);
            } else {
                Context.getPermissionsManager().checkAdmin(getUserId());
            }
        } else {
            Context.getPermissionsManager().checkPermission(ownerClass, getUserId(), ownerId);
        }
        Context.getPermissionsManager().checkPermission(propertyClass, getUserId(), propertyId);

        Context.getDataManager().linkObject(ownerClass, ownerId, propertyClass, propertyId, link);
    }

    protected void linkNewEntity(BaseModel entity) throws SQLException {
        Context.getDataManager().linkObject(User.class, getUserId(), entity.getClass(), entity.getId(), true);
    }
}
