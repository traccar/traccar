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
    public void testBetween() {
        Assert.assertEquals(0, BitUtil.between(0, 0, 0));
        Assert.assertEquals(1, BitUtil.between(1, 0, 1));
        Assert.assertEquals(2, BitUtil.between(2, 0, 2));
        Assert.assertEquals(2, BitUtil.between(6, 0, 2));
    }

    @Test
    public void testFrom() {
        Assert.assertEquals(1, BitUtil.from(1, 0));
        Assert.assertEquals(0, BitUtil.from(1, 1));
    }

    @Test
    public void testTo() {
        Assert.assertEquals(2, BitUtil.to(2, 2));
        Assert.assertEquals(0, BitUtil.to(2, 1));
    }

}
