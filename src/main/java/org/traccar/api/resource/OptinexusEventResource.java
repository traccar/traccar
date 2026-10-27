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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.BaseResource;
import org.traccar.database.OptinexusTenantEvents;
import org.traccar.database.OptinexusVendorRecords;
import org.traccar.database.OptinexusWebhookVerifier;
import org.traccar.storage.StorageException;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Receives the signed webhooks of the OptiNexus event broker. There is no session: the signature is the only
 * credential. A refusal is a 4xx, which OptiNexus does not retry; a storage failure is a 5xx, which it does.
 */
@Path("optinexus/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OptinexusEventResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptinexusEventResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private OptinexusWebhookVerifier verifier;

    @Inject
    private OptinexusTenantEvents events;

    @Inject
    private OptinexusVendorRecords vendorRecords;

    @PermitAll
    @POST
    public Response receive(
            String body,
            @HeaderParam("Opti-Webhook-Id") String deliveryId,
            @HeaderParam("Opti-Webhook-Timestamp") String timestamp,
            @HeaderParam("Opti-Webhook-Signature") String signature) {
        if (!verifier.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "not_configured")).build();
        }
        try {
            verifier.verify(deliveryId, timestamp, body, signature);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("OptiNexus webhook refused: {}", e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "invalid_signature")).build();
        }
        try {
            String type = MAPPER.readTree(body).path("event_type").asText();
            String result = OptinexusVendorRecords.handles(type) ? vendorRecords.handle(body) : events.handle(body);
            return Response.accepted(Map.of("result", result)).build();
        } catch (StorageException e) {
            LOGGER.error("OptiNexus webhook could not be applied", e);
            return Response.serverError().entity(Map.of("error", "storage")).build();
        } catch (IllegalArgumentException | IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "invalid_event")).build();
        }
    }

}
