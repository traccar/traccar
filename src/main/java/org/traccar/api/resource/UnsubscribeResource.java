/*
 * Copyright 2021 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.api.signature.TokenManager;
import org.traccar.api.signature.TokenPurpose;
import org.traccar.helper.LogAction;
import org.traccar.model.Notification;
import org.traccar.model.ObjectOperation;
import org.traccar.model.User;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collection;

@Path("unsubscribe")
@Produces(MediaType.APPLICATION_JSON)
public class UnsubscribeResource extends BaseResource {

    @Inject
    private TokenManager tokenManager;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private LogAction actionLogger;

    @Context
    private HttpServletRequest request;

    @GET
    @PermitAll
    public Collection<Notification> get(@QueryParam("token") String token) throws Exception {
        long userId = tokenManager.verifyToken(token, TokenPurpose.NOTIFICATIONS).getUserId();
        return storage.getObjects(Notification.class, new Request(
                new Columns.Include("id", "description", "type", "notificators"),
                new Condition.Permission(User.class, userId, Notification.class)));
    }

    @DELETE
    @Path("{id}")
    @PermitAll
    public Response remove(@QueryParam("token") String token, @PathParam("id") long id) throws Exception {
        long userId = tokenManager.verifyToken(token, TokenPurpose.NOTIFICATIONS).getUserId();
        storage.removeObject(Notification.class, new Request(new Condition.And(
                new Condition.Equals("id", id), new Condition.Permission(User.class, userId, Notification.class))));
        cacheManager.invalidateObject(true, Notification.class, id, ObjectOperation.DELETE);
        actionLogger.remove(request, userId, Notification.class, id);
        return Response.noContent().build();
    }

}
