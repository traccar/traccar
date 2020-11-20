package org.traccar.geofence;

import java.text.ParseException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeofencePolygonTest {

    @Test
    public void testCalculateArea() throws ParseException {
        String test = "POLYGON((-23.559204099194772 148.8653145299711, -23.6000443437826 148.85956016213583, -23.600411843430095 148.89462111436828, -23.5626384786532 148.90278297873897, -23.5574863232753 148.88137329347367, -23.559204099194772 148.8653145299711))";
        assertEquals(17, new GeofencePolygon(test).calculateArea(), 1);
    }

    @Test
    public void testPolygonWkt() throws ParseException {
        String test = "POLYGON ((55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        geofenceGeometry.fromWkt(test);
        assertEquals(geofenceGeometry.toWkt(), test);
    }

    @Test
    public void testContainsPolygon() throws ParseException {
        String test = "POLYGON ((55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        geofenceGeometry.fromWkt(test);
        assertTrue(geofenceGeometry.containsPoint(55.75476, 37.61915));
        assertFalse(geofenceGeometry.containsPoint(55.75545, 37.61921));

    }
    
    @Test
    public void testContainsPolygon180() throws ParseException {
        String test = "POLYGON ((66.9494 179.838, 66.9508 -179.8496, 66.8406 -180.0014))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        geofenceGeometry.fromWkt(test);
        assertTrue(geofenceGeometry.containsPoint(66.9015, -180.0096));
        assertTrue(geofenceGeometry.containsPoint(66.9015, 179.991));
        assertFalse(geofenceGeometry.containsPoint(66.8368, -179.8792));

    }
    
    @Test
    public void testContainsPolygon0() throws ParseException {
        String test = "POLYGON ((51.1966 -0.6207, 51.1897 0.4147, 50.9377 0.5136, 50.8675 -0.6082))";
        GeofenceGeometry geofenceGeometry = new GeofencePolygon();
        geofenceGeometry.fromWkt(test);
        assertTrue(geofenceGeometry.containsPoint(51.0466, -0.0165));
        assertTrue(geofenceGeometry.containsPoint(51.0466, 0.018));
        assertFalse(geofenceGeometry.containsPoint(50.9477, 0.5836));

    }

}
