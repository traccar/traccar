package org.traccar;

import org.junit.Test;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class DistanceHandlerTest {

    @Test
    public void testCalculateDistance() throws Exception {

        DistanceHandler distanceHandler = new DistanceHandler(false, 0, 0);

        Position position = distanceHandler.handlePosition(new Position());

        assertEquals(0.0, position.getAttributes().get(Position.KEY_DISTANCE));
        assertEquals(0.0, position.getAttributes().get(Position.KEY_TOTAL_DISTANCE));

        position.set(Position.KEY_DISTANCE, 100);

        position = distanceHandler.handlePosition(position);

        assertEquals(100.0, position.getAttributes().get(Position.KEY_DISTANCE));
        assertEquals(100.0, position.getAttributes().get(Position.KEY_TOTAL_DISTANCE));

    }

}
