package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

public class GeofencePolylineTest {

    @Test
    public void testPolylineWKT() {
        String test = "LINESTRING (55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline();
        try {
        geofenceGeometry.fromWkt(test);
        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }
        Assert.assertEquals(geofenceGeometry.toWkt(), test);
    }
    
    @Test
    public void testContainsPolyline1Interval() {
        String test = "LINESTRING (56.83777 60.59833, 56.83766 60.5968)";

        try {
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test, 35);
        Assert.assertTrue(geofenceGeometry.containsPoint(56.83801, 60.59748));

        ((GeofencePolyline) geofenceGeometry).setDistance(15);
        Assert.assertTrue(!geofenceGeometry.containsPoint(56.83801, 60.59748));

        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }
    }


    @Test
    public void testContainsPolyline3Intervals() {
        String test = "LINESTRING (56.836 60.6126, 56.8393 60.6114, 56.83887 60.60811, 56.83782 60.5988)";

        try {
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test, 15);
        Assert.assertTrue(geofenceGeometry.containsPoint(56.83847, 60.60458));
        Assert.assertTrue(!geofenceGeometry.containsPoint(56.83764, 60.59725));
        Assert.assertTrue(!geofenceGeometry.containsPoint(56.83861, 60.60822));

        } catch (ParseException e){
            Assert.assertTrue("ParseExceprion: " + e.getMessage(), true);
        }
    }

}
