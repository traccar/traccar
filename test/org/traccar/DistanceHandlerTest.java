package org.traccar;

import org.junit.Test;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class DistanceHandlerTest {

    @Test
    public void testCalculateDistance() throws Exception {

        DistanceHandler distanceHandler = new DistanceHandler();

        Position position = distanceHandler.calculateDistance(new Position());

        assertEquals(0.0, position.getAttributes().get(Position.KEY_DISTANCE));
        assertEquals(0.0, position.getAttributes().get(Position.KEY_TOTAL_DISTANCE));

    }

}
