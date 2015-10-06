package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class ChecksumTest {
    
    @Test
    public void testLuhnChecksum() {

        Assert.assertEquals(7, Checksum.luhnChecksum(12345678901234L));
        Assert.assertEquals(0, Checksum.luhnChecksum(63070019470771L));

    }

}
