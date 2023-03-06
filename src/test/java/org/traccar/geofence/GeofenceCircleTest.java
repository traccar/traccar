package org.traccar.geofence;

import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeofenceCircleTest {

    @Test
    public void testCircleWkt() throws ParseException {
        String test = "CIRCLE (55.75414 37.6204, 100)";
        GeofenceGeometry geofenceGeometry = new GeofenceCircle(test);
        assertEquals(geofenceGeometry.toWkt(), test);
    }

    @Test
    public void testContainsCircle() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofenceCircle("CIRCLE (55.75414 37.6204, 100)");
        assertTrue(geofenceGeometry.containsPoint(null, null, 55.75477, 37.62025));
        assertFalse(geofenceGeometry.containsPoint(null, null, 55.75545, 37.61921));
    }

}
