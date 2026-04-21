/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

@Path("health")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class HealthResource extends BaseResource {

    @Inject
    private Config config;

    @Inject
    private StatisticsManager statisticsManager;

    @Inject
    private Storage storage;

    private static long messageLastTotal;
    private static long messageLastCheck;

    @PermitAll
    @GET
    public Response health() {
        try {
            checkMessages();
            checkDatabase();
            return Response.ok("OK").build();
        } catch (Exception ignore) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
        }
    }

    private void checkMessages() {
        synchronized (HealthResource.class) {
            long messageCurrentTotal = statisticsManager.messageStoredCount();
            long messageCurrentCheck = messageCurrentTotal - messageLastTotal;
            double dropThreshold = config.getDouble(Keys.WEB_HEALTH_CHECK_DROP_THRESHOLD);
            if (dropThreshold > 0 && messageLastCheck > 0 && messageCurrentCheck > 0) {
                double ratio = messageCurrentCheck / (double) messageLastCheck;
                if (ratio < dropThreshold) {
                    throw new IllegalStateException("Message health check failed with ratio " + ratio);
                }
            }
            messageLastTotal = messageCurrentTotal;
            messageLastCheck = messageCurrentCheck;
        }
    }

    private void checkDatabase() throws StorageException {
        storage.getObject(Server.class, new Request(new Columns.All()));
    }

}
