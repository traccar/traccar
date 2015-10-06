package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class ChecksumTest {
    
    @Test
    public void testLuhnChecksum() {

        Assert.assertEquals(7, Checksum.luhn(12345678901234L));
        Assert.assertEquals(0, Checksum.luhn(63070019470771L));

    }

}
