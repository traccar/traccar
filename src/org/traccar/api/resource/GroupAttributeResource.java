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
package org.traccar.api.resource;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.GroupAttribute;

@Path("groups/attributes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupAttributeResource extends BaseResource {

    @POST
    public Response add(GroupAttribute entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkGroup(getUserId(), entity.getGroupId());
        Context.getPermissionsManager().checkAttribute(getUserId(), entity.getAttributeId());
        Context.getDataManager().linkGroupAttribute(entity.getGroupId(), entity.getAttributeId());
        Context.getAttributesManager().refresh();
        return Response.ok(entity).build();
    }

    @DELETE
    public Response remove(GroupAttribute entity) throws SQLException {
        Context.getPermissionsManager().checkReadonly(getUserId());
        Context.getPermissionsManager().checkGroup(getUserId(), entity.getGroupId());
        Context.getPermissionsManager().checkGeofence(getUserId(), entity.getAttributeId());
        Context.getDataManager().unlinkGroupAttribute(entity.getGroupId(), entity.getAttributeId());
        Context.getAttributesManager().refresh();
        return Response.noContent().build();
    }

}
