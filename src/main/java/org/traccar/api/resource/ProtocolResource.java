/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.ServerManager;
import org.traccar.api.BaseResource;
import org.traccar.helper.LogAction;
import org.traccar.model.ProtocolSettings;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("protocols")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProtocolResource extends BaseResource {

    @Inject
    private ServerManager serverManager;

    @Inject
    private LogAction actionLogger;

    @Context
    private HttpServletRequest request;

    @GET
    public List<ProtocolSettings> get() throws StorageException {
        return storage.getObjects(ProtocolSettings.class, new Request(new Columns.All(), new Order("protocol")));
    }

    @Path("{id}")
    @PUT
    public Response update(@PathParam("id") long id, ProtocolSettings entity) throws Exception {
        permissionsService.checkAdmin(getUserId());
        ProtocolSettings settings = storage.getObject(ProtocolSettings.class, new Request(
                new Columns.All(), new Condition.Equals("id", id)));
        settings.setPort(entity.getPort());
        settings.setTransport(entity.getTransport());
        serverManager.updateSettings(settings);
        actionLogger.edit(request, getUserId(), settings);
        return Response.ok(settings).build();
    }

}
