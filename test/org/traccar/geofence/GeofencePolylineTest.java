package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeofencePolylineTest {

    @Test
    public void testPolylineWkt() throws ParseException {
        String test = "LINESTRING (55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline();
        geofenceGeometry.fromWkt(test);
        assertEquals(geofenceGeometry.toWkt(), test);
    }
    
    @Test
    public void testContainsPolyline1Interval() throws ParseException {
        String test = "LINESTRING (56.83777 60.59833, 56.83766 60.5968)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test, 35);
        assertTrue(geofenceGeometry.containsPoint(56.83801, 60.59748));
        ((GeofencePolyline) geofenceGeometry).setDistance(15);
        assertTrue(!geofenceGeometry.containsPoint(56.83801, 60.59748));
    }

    @Test
    public void testContainsPolyline3Intervals() throws ParseException {
        String test = "LINESTRING (56.836 60.6126, 56.8393 60.6114, 56.83887 60.60811, 56.83782 60.5988)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test, 15);
        assertTrue(geofenceGeometry.containsPoint(56.83847, 60.60458));
        assertTrue(!geofenceGeometry.containsPoint(56.83764, 60.59725));
        assertTrue(!geofenceGeometry.containsPoint(56.83861, 60.60822));

    }
    
    @Test
    public void testContainsPolylineNear180() throws ParseException {
        String test = "LINESTRING (66.9494 179.838, 66.9508 -179.8496)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test, 25);
        assertTrue(geofenceGeometry.containsPoint(66.95, 180.0));
        assertTrue(!geofenceGeometry.containsPoint(66.96, 180.0));
        assertTrue(!geofenceGeometry.containsPoint(66.9509, -179.83));
    }
}
