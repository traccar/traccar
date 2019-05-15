package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.PeripheralSensor;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Path("peripheralsensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PeripheralSensorResource extends BaseObjectResource<PeripheralSensor> {

    public PeripheralSensorResource() {
        super(PeripheralSensor.class);
    }

    @PermitAll
    @GET
    public Collection<PeripheralSensor> get(@QueryParam("deviceId") Long deviceId) {
        Optional<List<PeripheralSensor>> sensors = Context.getPeripheralSensorManager().getSensorByDeviceId(deviceId);

        if (sensors.isPresent()) {
            return sensors.get();
        }
        return null;
    }
}
