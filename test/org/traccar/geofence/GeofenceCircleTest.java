package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

public class GeofenceCircleTest {

    @Test
    public void testCircleWKT() {
        String test = "CIRCLE (55.75414 37.6204, 100)";
        GeofenceGeometry geofenceGeometry = new GeofenceCircle();
        try {
        geofenceGeometry.fromWkt(test);
        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }
        Assert.assertEquals(geofenceGeometry.toWkt(), test);
    }

    @Test
    public void testContainsCircle() {
        String test = "CIRCLE (55.75414 37.6204, 100)";
        GeofenceGeometry geofenceGeometry = new GeofenceCircle();
        try {
        geofenceGeometry.fromWkt(test);
        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }

        Assert.assertTrue(geofenceGeometry.containsPoint(55.75477, 37.62025));

        Assert.assertTrue(!geofenceGeometry.containsPoint(55.75545, 37.61921));
    }
}
