package org.traccar.processing;

import org.junit.Test;
import org.traccar.Context;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class SensorDataHandlerTest {

    @Test
    public void testDigitalFuelSensorData() {

        Position position = new Position();
        position.setDeviceId(1);
        position.set("sensorId", 1);
        position.set("sensorData", "F=0BC0 t=19 N=043F.0");

        SensorDataHandler sensorDataHandler = new SensorDataHandler();

        sensorDataHandler.handlePosition(position);

        assertEquals(1087L, position.getAttributes().get(Position.KEY_FUEL_LEVEL));

    }
}