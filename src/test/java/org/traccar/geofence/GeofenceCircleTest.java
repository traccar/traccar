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
        assertEquals(test, geofenceGeometry.toWkt());
    }

    @Test
    public void testContainsCircle() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofenceCircle("CIRCLE (55.75414 37.6204, 100)");
        assertTrue(geofenceGeometry.containsPoint(55.75477, 37.62025));
        assertFalse(geofenceGeometry.containsPoint(55.75545, 37.61921));
    }

    @Test
    public void testIntersectsCircle() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofenceCircle("CIRCLE (55.75414 37.6204, 100)");
        assertTrue(geofenceGeometry.intersectsSegment(55.75214, 37.6204, 55.75614, 37.6204));
        assertFalse(geofenceGeometry.intersectsSegment(55.76414, 37.6204, 55.77414, 37.6204));
    }

}
