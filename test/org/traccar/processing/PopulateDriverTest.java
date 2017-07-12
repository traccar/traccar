package org.traccar.processing;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Position;

public class PopulateDriverTest {

    @Test
    public void testPopulateDriver() {
        Position position = new Position();
        PopulateDriverHandler populateDriverHandler = new PopulateDriverHandler();
        Assert.assertNull(populateDriverHandler.handlePosition(position).getString(Position.KEY_DRIVER_UNIQUE_ID));
        position.set(Position.KEY_DRIVER_UNIQUE_ID, "123");
        Assert.assertEquals("123",
                populateDriverHandler.handlePosition(position).getString(Position.KEY_DRIVER_UNIQUE_ID));
        position.set(Position.KEY_RFID, "321");
        Assert.assertEquals("321",
                populateDriverHandler.handlePosition(position).getString(Position.KEY_DRIVER_UNIQUE_ID));
    }
}
