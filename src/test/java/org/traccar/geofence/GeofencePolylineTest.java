package org.traccar.geofence;

import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Geofence;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeofencePolylineTest {

    @Test
    public void testPolylineWkt() throws ParseException {
        String test = "LINESTRING (55.75474 37.61823, 55.75513 37.61888, 55.7535 37.6222, 55.75315 37.62165)";
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(test);
        assertEquals(geofenceGeometry.toWkt(), test);
    }
    
    @Test
    public void testContainsPolyline1Interval() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(
                "LINESTRING (56.83777 60.59833, 56.83766 60.5968)");
        Config config = mock(Config.class);
        when(config.getDouble(Keys.GEOFENCE_POLYLINE_DISTANCE)).thenReturn(35.0);
        assertTrue(geofenceGeometry.containsPoint(config, mock(Geofence.class), 56.83801, 60.59748));
        when(config.getDouble(Keys.GEOFENCE_POLYLINE_DISTANCE)).thenReturn(15.0);
        assertFalse(geofenceGeometry.containsPoint(config, mock(Geofence.class), 56.83801, 60.59748));
    }

    @Test
    public void testContainsPolyline3Intervals() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(
                "LINESTRING (56.836 60.6126, 56.8393 60.6114, 56.83887 60.60811, 56.83782 60.5988)");
        Config config = mock(Config.class);
        when(config.getDouble(Keys.GEOFENCE_POLYLINE_DISTANCE)).thenReturn(15.0);
        assertTrue(geofenceGeometry.containsPoint(config, mock(Geofence.class), 56.83847, 60.60458));
        assertFalse(geofenceGeometry.containsPoint(config, mock(Geofence.class), 56.83764, 60.59725));
        assertFalse(geofenceGeometry.containsPoint(config, mock(Geofence.class), 56.83861, 60.60822));

    }
    
    @Test
    public void testContainsPolylineNear180() throws ParseException {
        GeofenceGeometry geofenceGeometry = new GeofencePolyline(
                "LINESTRING (66.9494 179.838, 66.9508 -179.8496)");
        Config config = mock(Config.class);
        when(config.getDouble(Keys.GEOFENCE_POLYLINE_DISTANCE)).thenReturn(25.0);
        assertTrue(geofenceGeometry.containsPoint(config, mock(Geofence.class), 66.95, 180.0));
        assertFalse(geofenceGeometry.containsPoint(config, mock(Geofence.class), 66.96, 180.0));
        assertFalse(geofenceGeometry.containsPoint(config, mock(Geofence.class), 66.9509, -179.83));
    }

}
