package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class PatternBuilderTest {
    
    @Test
    public void testPatternBuilder() {
        Assert.assertEquals("\\$GPRMC", new PatternBuilder().txt("$GPRMC").toString());
        Assert.assertEquals("(\\d{2}\\.\\p{XDigit}+)", new PatternBuilder().num("(dd.x+)").toString());
    }

}
