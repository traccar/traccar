package org.traccar.handler;

import org.junit.Test;
import org.traccar.model.Position;
import org.traccar.reports.common.TripsConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MotionHandlerTest {

    @Test
    public void testCalculateMotion() {

        TripsConfig tripsConfig = mock(TripsConfig.class);
        when(tripsConfig.getSpeedThreshold()).thenReturn(0.01);

        MotionHandler motionHandler = new MotionHandler(tripsConfig);

        Position position = motionHandler.handlePosition(new Position());

        assertEquals(false, position.getAttributes().get(Position.KEY_MOTION));

    }

}
