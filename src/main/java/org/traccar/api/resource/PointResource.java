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

import org.traccar.api.SimpleObjectResource;
import org.traccar.model.Point;
import org.traccar.reports.KmlExportProvider;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.List;
import javax.xml.stream.XMLStreamException;

@Path("points")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PointResource extends SimpleObjectResource<Point> {

    @Inject
    private KmlExportProvider kmlExportProvider;

    public PointResource() {
        super(Point.class, "name", List.of("name"));
    }

    @Path("kml")
    @GET
    @Produces("application/vnd.google-earth.kml+xml")
    public Response getKml() {
        long userId = getUserId();
        StreamingOutput stream = output -> {
            try {
                kmlExportProvider.generatePoints(output, userId);
            } catch (XMLStreamException | StorageException e) {
                throw new WebApplicationException(e);
            }
        };
        return Response.ok(stream).build();
    }

}
