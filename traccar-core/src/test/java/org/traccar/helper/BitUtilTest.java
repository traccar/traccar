package org.traccar.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
