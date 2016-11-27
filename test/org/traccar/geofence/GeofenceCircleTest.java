package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

public class GeofenceCircleTest {

    @Test
    public void testCircleWkt() throws ParseException {
        String test = "CIRCLE (55.75414 37.6204, 100)";
        GeofenceGeometry geofenceGeometry = new GeofenceCircle();
        geofenceGeometry.fromWkt(test);
        Assert.assertEquals(geofenceGeometry.toWkt(), test);
    }

    @Test
    public void testContainsCircle() throws ParseException {
        String test = "CIRCLE (55.75414 37.6204, 100)";
        GeofenceGeometry geofenceGeometry = new GeofenceCircle();
        geofenceGeometry.fromWkt(test);
        Assert.assertTrue(geofenceGeometry.containsPoint(55.75477, 37.62025));
        Assert.assertTrue(!geofenceGeometry.containsPoint(55.75545, 37.61921));
    }
}
