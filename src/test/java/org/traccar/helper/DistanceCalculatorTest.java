package org.traccar.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DistanceCalculatorTest {

    @Test
    public void testDistance() {
        assertEquals(7863.0, DistanceCalculator.distance(
                0.0, 0.0, 0.05, 0.05), 10.0);
    }

    @Test
    public void testDistanceToLine() {
        assertEquals(33.0, DistanceCalculator.distanceToLine(
                56.83801, 60.59748, 56.83777, 60.59833, 56.83766, 60.5968), 5.0);

        assertEquals(105.0, DistanceCalculator.distanceToLine(
                56.83753, 60.59508, 56.83777, 60.59833, 56.83766, 60.5968), 5.0);
    }

    @Test
    public void testSegmentsIntersect() {
        assertTrue(DistanceCalculator.segmentsIntersect(
                0, 0, 2, 2,
                0, 2, 2, 0));

        assertFalse(DistanceCalculator.segmentsIntersect(
                0, 0, 1, 1,
                2, 2, 3, 3));

        assertTrue(DistanceCalculator.segmentsIntersect(
                0, 0, 2, 2,
                2, 2, 4, 0));

        assertTrue(DistanceCalculator.segmentsIntersect(
                0, 0, 2, 0,
                1, 0, 3, 0));
    }

}
