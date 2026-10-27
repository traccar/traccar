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

import org.traccar.api.BaseResource;
import org.traccar.database.OptinexusVehicleMaster;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Administrator action to share the vehicles of all tenants with the other platforms again (first start-up of the
 * sync, or after an outage). Saving a device already shares its vehicle, so this is only needed for devices that
 * existed before.
 */
@Path("optinexus/vehicles")
@Produces(MediaType.APPLICATION_JSON)
public class OptinexusVehicleResource extends BaseResource {

    @Inject
    private OptinexusVehicleMaster vehicleMaster;

    @Path("publish")
    @POST
    public Map<String, Integer> publish() throws StorageException {
        permissionsService.checkAdmin(getUserId());
        return Map.of("queued", vehicleMaster.publishAll());
    }

}
