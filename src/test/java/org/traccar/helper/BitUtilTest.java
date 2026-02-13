package org.traccar.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitUtilTest {
    
    @Test
    public void testCheck() {
        assertFalse(BitUtil.check(0, 0));
        assertTrue(BitUtil.check(1, 0));
        assertFalse(BitUtil.check(2, 0));
    }
    
    @Test
    public void testBetween() {
        assertEquals(0, BitUtil.between(0, 0, 0));
        assertEquals(1, BitUtil.between(1, 0, 1));
        assertEquals(2, BitUtil.between(2, 0, 2));
        assertEquals(2, BitUtil.between(6, 0, 2));
    }

    @Test
    public void testFrom() {
        assertEquals(1, BitUtil.from(1, 0));
        assertEquals(0, BitUtil.from(1, 1));
    }

    @Test
    public void testTo() {
        assertEquals(2, BitUtil.to(2, 2));
        assertEquals(0, BitUtil.to(2, 1));
    }

}
