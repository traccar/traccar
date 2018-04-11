package org.traccar.processing;

import org.junit.Test;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class PeripheralSensorDataHandlerTest {

    @Test
    public void testDigitalFuelSensorData() {

        Position position = new Position();
        position.setDeviceId(1);
        position.set("sensorId", 1);
        position.set("sensorData", "F=0BC0 t=19 N=043F.0");

        PeripheralSensorDataHandler peripheralSensorDataHandler = new PeripheralSensorDataHandler();

        peripheralSensorDataHandler.handlePosition(position);

        assertEquals(1087L, position.getAttributes().get(Position.KEY_FUEL_LEVEL));

    }
}