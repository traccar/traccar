package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

public class GeofencePolygonTest {

    @Test
    public void testPolygonWKT() {
        String test = "POLYGON ((55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        try {
        geofenceGeometry.fromWkt(test);
        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }
        Assert.assertEquals(geofenceGeometry.toWkt(), test);
    }

    @Test
    public void testContainsPolygon() {
        String test = "POLYGON ((55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        try {
        geofenceGeometry.fromWkt(test);
        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }

        Assert.assertTrue(geofenceGeometry.containsPoint(55.75476, 37.61915));

        Assert.assertTrue(!geofenceGeometry.containsPoint(55.75545, 37.61921));

    }

}
