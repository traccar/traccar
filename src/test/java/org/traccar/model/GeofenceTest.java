package org.traccar.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traccar.geofence.GeofenceCircle;
import org.traccar.geofence.GeofenceGeometry;
import org.traccar.geofence.GeofencePolygon;
import org.traccar.geofence.GeofencePolyline;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeofenceTest {

    private static final String CIRCLE = "CIRCLE (55.75414 37.6204, 100)";
    private static final String POLYGON = "POLYGON ((55.75 37.61, 55.76 37.61, 55.76 37.63, 55.75 37.63))";
    private static final String LINESTRING = "LINESTRING (56.83777 60.59833, 56.83766 60.5968)";

    // points relative to CIRCLE
    private static final double IN_LAT = 55.75477;
    private static final double IN_LON = 37.62025;
    private static final double OUT_LAT = 55.75545;
    private static final double OUT_LON = 37.61921;

    private Geofence geofence;

    @BeforeEach
    public void setUp() {
        geofence = new Geofence();
        geofence.setArea(CIRCLE);
    }

    private static Position position(double latitude, double longitude, double altitude) {
        Position position = new Position();
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setAltitude(altitude);
        return position;
    }

    // segment crossing straight through the circle center
    private static Position segmentStart(double altitude) {
        return position(55.75214, 37.6204, altitude);
    }

    private static Position segmentEnd(double altitude) {
        return position(55.75614, 37.6204, altitude);
    }

    // geometry selection

    @Test
    public void testCircleAreaCreatesCircleGeometry() {
        assertInstanceOf(GeofenceCircle.class, geofence.getGeometry());
    }

    @Test
    public void testPolygonAreaCreatesPolygonGeometry() {
        geofence.setArea(POLYGON);
        assertInstanceOf(GeofencePolygon.class, geofence.getGeometry());
    }

    @Test
    public void testLinestringAreaCreatesPolylineGeometry() {
        geofence.setArea(LINESTRING);
        assertInstanceOf(GeofencePolyline.class, geofence.getGeometry());
    }

    @Test
    public void testUnknownGeometryTypeThrows() {
        geofence.setArea("POINT (1 2)");
        assertThrows(IllegalArgumentException.class, geofence::getGeometry);
    }

    @Test
    public void testMalformedWktWrapsParseException() {
        geofence.setArea("CIRCLE (abc)");
        RuntimeException e = assertThrows(RuntimeException.class, geofence::getGeometry);
        assertInstanceOf(ParseException.class, e.getCause());
    }

    @Test
    public void testNullAreaThrowsNullPointerException() {
        Geofence empty = new Geofence();
        assertThrows(NullPointerException.class, empty::getGeometry);
    }

    // caching

    @Test
    public void testGeometryIsCached() {
        assertSame(geofence.getGeometry(), geofence.getGeometry());
    }

    @Test
    public void testSetAreaResetsCachedGeometry() {
        GeofenceGeometry first = geofence.getGeometry();
        geofence.setArea(CIRCLE);
        assertNotSame(first, geofence.getGeometry());
    }

    @Test
    public void testSetGeometryStoresWkt() throws ParseException {
        GeofenceGeometry geometry = new GeofencePolygon(POLYGON);
        geofence.setGeometry(geometry);
        assertEquals(geometry.toWkt(), geofence.getArea());
        assertInstanceOf(GeofencePolygon.class, geofence.getGeometry());
    }

    // containsPosition

    @Test
    public void testContainsPositionWithoutAltitudeLimits() {
        assertTrue(geofence.containsPosition(position(IN_LAT, IN_LON, 0)));
        assertFalse(geofence.containsPosition(position(OUT_LAT, OUT_LON, 0)));
    }

    @ParameterizedTest
    @CsvSource({
            "99.9, false",
            "100.0, true",
            "150.0, true",
    })
    public void testFloorBoundary(double altitude, boolean expected) {
        geofence.set("floor", 100.0);
        assertEquals(expected, geofence.containsPosition(position(IN_LAT, IN_LON, altitude)));
    }

    @ParameterizedTest
    @CsvSource({
            "150.0, true",
            "200.0, true",
            "200.1, false",
    })
    public void testCeilingBoundary(double altitude, boolean expected) {
        geofence.set("ceiling", 200.0);
        assertEquals(expected, geofence.containsPosition(position(IN_LAT, IN_LON, altitude)));
    }

    @ParameterizedTest
    @CsvSource({
            "99.9, false",
            "100.0, true",
            "200.0, true",
            "200.1, false",
    })
    public void testFloorAndCeilingTogether(double altitude, boolean expected) {
        geofence.set("floor", 100.0);
        geofence.set("ceiling", 200.0);
        assertEquals(expected, geofence.containsPosition(position(IN_LAT, IN_LON, altitude)));
    }

    @Test
    public void testZeroFloorMeansNoFloor() {
        geofence.set("floor", 0.0);
        assertTrue(geofence.containsPosition(position(IN_LAT, IN_LON, -10.0)));
    }

    @Test
    public void testZeroCeilingMeansNoCeiling() {
        geofence.set("ceiling", 0.0);
        assertTrue(geofence.containsPosition(position(IN_LAT, IN_LON, 10000.0)));
    }

    @Test
    public void testAltitudeInRangeButOutside2D() {
        geofence.set("floor", 100.0);
        geofence.set("ceiling", 200.0);
        assertFalse(geofence.containsPosition(position(OUT_LAT, OUT_LON, 150.0)));
    }

    @Test
    public void testFloorBelowZero() {
        geofence.set("floor", -50.0);
        assertTrue(geofence.containsPosition(position(IN_LAT, IN_LON, -40.0)));
    }

    // containsSegment

    @Test
    public void testSegmentWithoutAltitudeLimits() {
        assertTrue(geofence.containsSegment(segmentStart(0), segmentEnd(0)));
    }

    @Test
    public void testSegmentOutside2D() {
        Position from = position(OUT_LAT, OUT_LON, 0);
        Position to = position(OUT_LAT + 0.001, OUT_LON - 0.001, 0);
        assertFalse(geofence.containsSegment(from, to));
    }

    @Test
    public void testSegmentBothEndsBelowFloor() {
        geofence.set("floor", 100.0);
        assertFalse(geofence.containsSegment(segmentStart(50.0), segmentEnd(50.0)));
    }

    @Test
    public void testSegmentBothEndsAboveFloor() {
        geofence.set("floor", 100.0);
        assertTrue(geofence.containsSegment(segmentStart(150.0), segmentEnd(150.0)));
    }

    @Test
    public void testSegmentOneEndBelowFloor() {
        geofence.set("floor", 100.0);
        assertTrue(geofence.containsSegment(segmentStart(50.0), segmentEnd(150.0)));
    }

    @Test
    public void testSegmentBothEndsAboveCeiling() {
        geofence.set("ceiling", 200.0);
        assertFalse(geofence.containsSegment(segmentStart(250.0), segmentEnd(250.0)));
    }

    @Test
    public void testSegmentOneEndBelowCeiling() {
        geofence.set("ceiling", 100.0);
        assertTrue(geofence.containsSegment(segmentStart(150.0), segmentEnd(50.0)));
    }

    @Test
    public void testSegmentOneEndAboveCeiling() {
        geofence.set("ceiling", 200.0);
        assertTrue(geofence.containsSegment(segmentStart(150.0), segmentEnd(250.0)));
    }

    // polylineDistance

    @Test
    public void testPolylineUsesDefaultDistance() {
        geofence.setArea(LINESTRING);
        // roughly 44 m from the line, outside the default 25 m
        assertFalse(geofence.containsPosition(position(56.838115, 60.597565, 0)));
    }

    @Test
    public void testPolylineUsesDistanceAttribute() {
        geofence.set("polylineDistance", 100.0);
        geofence.setArea(LINESTRING);
        assertTrue(geofence.containsPosition(position(56.838115, 60.597565, 0)));
    }

}
