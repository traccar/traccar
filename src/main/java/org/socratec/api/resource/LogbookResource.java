/*
 * Copyright 2024 Socratec Telematic GmbH
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
package org.socratec.api.resource;

import org.traccar.api.BaseResource;
import org.socratec.model.Trip;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

@Path("logbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogbookResource extends BaseResource {

    @GET
    public Collection<Trip> get() {
        // Return 2 mock Trip instances
        Trip trip1 = new Trip(
                UUID.randomUUID().toString(),
                52.5200, // Berlin latitude
                13.4050  // Berlin longitude
        );

        Trip trip2 = new Trip(
                UUID.randomUUID().toString(),
                48.8566, // Paris latitude
                2.3522   // Paris longitude
        );

        return Arrays.asList(trip1, trip2);
    }
}
