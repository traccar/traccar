/*
 * Copyright 2016 Gabor Somogyi (gabor.g.somogyi@gmail.com)
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

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Position;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

@Path("supportedcommands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupportedCommandResource extends BaseResource {

    @GET
    public Collection<String> get(@QueryParam("deviceId") long deviceId) throws SQLException {
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        Position lastPosition = Context.getConnectionManager().getLastPosition(deviceId);
        if (lastPosition != null) {
            return Context.getServerManager().getProtocolSuppportedCommands(lastPosition.getProtocol());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

}
