package org.traccar.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternBuilderTest {
    
    @Test
    public void testPatternBuilder() {
        assertEquals("\\$GPRMC", new PatternBuilder().text("$GPRMC").toString());
        assertEquals("(\\d{2}\\.[0-9a-fA-F]+)", new PatternBuilder().number("(dd.x+)").toString());
        assertEquals("a(?:bc)?", new PatternBuilder().text("a").text("b").text("c").optional(2).toString());
        assertEquals("a|b", new PatternBuilder().expression("a|b").toString());
        assertEquals("ab\\|", new PatternBuilder().expression("ab|").toString());
        assertEquals("|", new PatternBuilder().or().toString());
        assertEquals("\\|\\d|\\d\\|", new PatternBuilder().number("|d|d|").toString());
    }

}
