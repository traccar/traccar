package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.database.CustomerLocationManager;
import org.traccar.model.CustomerLocation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.ParseException;
import java.util.Collection;

@Path("customerlocations")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerLocationResource extends BaseObjectResource<CustomerLocation> {

    public CustomerLocationResource() {
        super(CustomerLocation.class);
    }

    @GET
    public Collection<CustomerLocation> get() throws ParseException {
        CustomerLocationManager customerLocationManager = Context.getCustomerLocationManager();
        return customerLocationManager.getLocationByUserId(getUserId());
    }
}
