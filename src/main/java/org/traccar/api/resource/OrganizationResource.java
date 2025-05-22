package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.SimpleObjectResource;
import org.traccar.model.*;

@Path("organization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizationResource extends SimpleObjectResource<Organization> {

    public OrganizationResource() {
        super(Organization.class, "name");    }


}
