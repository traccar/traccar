/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.api.BaseResource;
import org.traccar.helper.LogAction;
import org.traccar.model.Permission;
import org.traccar.model.UserRestrictions;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Path("permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PermissionsResource  extends BaseResource {

    @Inject
    private CacheManager cacheManager;

    private void checkPermission(Permission permission) throws StorageException {
        if (permissionsService.notAdmin(getUserId())) {
            permissionsService.checkPermission(permission.getOwnerClass(), getUserId(), permission.getOwnerId());
            permissionsService.checkPermission(permission.getPropertyClass(), getUserId(), permission.getPropertyId());
        }
    }

    private void checkPermissionTypes(List<LinkedHashMap<String, Long>> entities) {
        Set<String> keys = null;
        for (LinkedHashMap<String, Long> entity: entities) {
            if (keys != null & !entity.keySet().equals(keys)) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
            keys = entity.keySet();
        }
    }

    @Path("bulk")
    @POST
    public Response add(List<LinkedHashMap<String, Long>> entities) throws Exception {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getReadonly);
        checkPermissionTypes(entities);
        for (LinkedHashMap<String, Long> entity: entities) {
            Permission permission = new Permission(entity);
            checkPermission(permission);
            storage.addPermission(permission);
            cacheManager.invalidatePermission(
                    true,
                    permission.getOwnerClass(), permission.getOwnerId(),
                    permission.getPropertyClass(), permission.getPropertyId(),
                    true);
            LogAction.link(getUserId(),
                    permission.getOwnerClass(), permission.getOwnerId(),
                    permission.getPropertyClass(), permission.getPropertyId());
        }
        return Response.noContent().build();
    }

    @POST
    public Response add(LinkedHashMap<String, Long> entity) throws Exception {
        return add(Collections.singletonList(entity));
    }

    @DELETE
    @Path("bulk")
    public Response remove(List<LinkedHashMap<String, Long>> entities) throws Exception {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getReadonly);
        checkPermissionTypes(entities);
        for (LinkedHashMap<String, Long> entity: entities) {
            Permission permission = new Permission(entity);
            checkPermission(permission);
            storage.removePermission(permission);
            cacheManager.invalidatePermission(
                    true,
                    permission.getOwnerClass(), permission.getOwnerId(),
                    permission.getPropertyClass(), permission.getPropertyId(),
                    false);
            LogAction.unlink(getUserId(),
                    permission.getOwnerClass(), permission.getOwnerId(),
                    permission.getPropertyClass(), permission.getPropertyId());
        }
        return Response.noContent().build();
    }

    @DELETE
    public Response remove(LinkedHashMap<String, Long> entity) throws Exception {
        return remove(Collections.singletonList(entity));
    }

}
