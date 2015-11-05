package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class ObdDecoderTest {
    
    @Test
    public void testDecode() {

        Assert.assertEquals(83, ObdDecoder.decode(0x01, 0x05, "7b").getValue());
        Assert.assertEquals(1225, ObdDecoder.decode(0x01, 0x0C, "1324").getValue());
        Assert.assertEquals(20, ObdDecoder.decode(0x01, 0x0D, "14").getValue());
        Assert.assertEquals(64050, ObdDecoder.decode(0x01, 0x31, "fa32").getValue());
        Assert.assertEquals(25, ObdDecoder.decode(0x01, 0x2F, "41").getValue());

    }

}
