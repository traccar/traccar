package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class BitUtilTest {
    
    @Test
    public void testCheck() {
        Assert.assertFalse(BitUtil.check(0, 0));
        Assert.assertTrue(BitUtil.check(1, 0));
        Assert.assertFalse(BitUtil.check(2, 0));
    }
    
    @Test
    public void testRange() {
        Assert.assertEquals(0, BitUtil.range(0, 0, 0));
        Assert.assertEquals(1, BitUtil.range(1, 0, 1));
        Assert.assertEquals(2, BitUtil.range(2, 0, 2));
        Assert.assertEquals(2, BitUtil.range(6, 0, 2));
    }

}
