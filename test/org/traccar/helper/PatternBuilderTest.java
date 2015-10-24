package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class PatternBuilderTest {
    
    @Test
    public void testPatternBuilder() {
        Assert.assertEquals("\\$GPRMC", new PatternBuilder().text("$GPRMC").toString());
        Assert.assertEquals("(\\d{2}\\.\\p{XDigit}+)", new PatternBuilder().number("(dd.x+)").toString());
        Assert.assertEquals("a(?:bc)?", new PatternBuilder().text("a").text("b").text("c").optional(2).toString());
    }

}
