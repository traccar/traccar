package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class DistanceCalculatorTest {
    
    @Test
    public void testDistance() {
        Assert.assertEquals(
                DistanceCalculator.distance(0.0, 0.0, 0.05, 0.05), 7863.0, 10.0);
    }

}
