package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class CrcTest {
    
    @Test
    public void testLuhnChecksum() {

        Assert.assertEquals(7, Crc.luhnChecksum(12345678901234L));
        Assert.assertEquals(0, Crc.luhnChecksum(63070019470771L));

    }

}
