/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import org.traccar.Context;
import org.traccar.database.DataManager;
import org.traccar.model.BaseModel;

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

    protected void handlePermission(Map<String, Long> entity, boolean link) throws SQLException {
        if (entity.size() != 2) {
            throw new IllegalArgumentException();
        }
        Iterator<String> iterator = entity.keySet().iterator();
        String owner = iterator.next();
        String property = iterator.next();

        long ownerId = entity.get(owner);
        long propertyId = entity.get(property);

        if (!link && DataManager.makeName(owner).equals(Context.TYPE_USER)
                && DataManager.makeName(property).equals(Context.TYPE_DEVICE)) {
            if (getUserId() != ownerId) {
                Context.getPermissionsManager().checkUser(getUserId(), ownerId);
            } else {
                Context.getPermissionsManager().checkAdmin(getUserId());
            }
        } else {
            Context.getPermissionsManager().checkPermission(owner, getUserId(), ownerId);
        }
        Context.getPermissionsManager().checkPermission(property, getUserId(), propertyId);

        Context.getDataManager().linkObject(owner, ownerId, property, propertyId, link);
    }

    protected void linkNew(BaseModel entity) throws SQLException {
        Context.getDataManager().linkObject("userId", getUserId(),
                entity.getClass().getSimpleName(), entity.getId(), true);
    }
}
