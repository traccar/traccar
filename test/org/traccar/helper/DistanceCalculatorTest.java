package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class DistanceCalculatorTest {
    
    @Test
    public void testDistance() {
        Assert.assertEquals(
                DistanceCalculator.distance(0.0, 0.0, 0.05, 0.05), 7863.0, 10.0);
    }
    
    @Test
    public void testDistanceToLine() {
        Assert.assertEquals(DistanceCalculator.distanceToLine(
                56.83801, 60.59748, 56.83777, 60.59833, 56.83766, 60.5968), 33.0, 5.0);
        
        Assert.assertEquals(DistanceCalculator.distanceToLine(
                56.83753, 60.59508, 56.83777, 60.59833, 56.83766, 60.5968), 105.0, 5.0);
    }

}
