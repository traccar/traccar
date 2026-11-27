package org.traccar.helper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternUtilTest {

    @Disabled
    @Test
    public void testCheckPattern() {

        assertEquals("ab", PatternUtil.checkPattern("abc", "abd").getPatternMatch());

    }

}
