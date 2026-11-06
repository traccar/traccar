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

import org.traccar.api.SimpleObjectResource;
import org.traccar.model.Geofence;
import org.traccar.model.Order;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource extends SimpleObjectResource<Order> {

    public OrderResource() {
        super(Order.class, "description", List.of("description"));
    }

    private void checkGeofencePermission(Order order) throws Exception {
        if (order.getGeofenceId() > 0) {
            permissionsService.checkPermission(Geofence.class, getUserId(), order.getGeofenceId());
        }
    }

    @Override
    public Response add(Order entity) throws Exception {
        checkGeofencePermission(entity);
        return super.add(entity);
    }

    @Override
    public Response update(Order entity) throws Exception {
        checkGeofencePermission(entity);
        return super.update(entity);
    }

}
