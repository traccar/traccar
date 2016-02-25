package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class ObdDecoderTest {
    
    @Test
    public void testDecode() {

        Assert.assertEquals(83, ObdDecoder.decode(0x01, "057b").getValue());
        Assert.assertEquals(1225, ObdDecoder.decode(0x01, "0C1324").getValue());
        Assert.assertEquals(20, ObdDecoder.decode(0x01, "0D14").getValue());
        Assert.assertEquals(64050, ObdDecoder.decode(0x01, "31fa32").getValue());
        Assert.assertEquals(25, ObdDecoder.decode(0x01, "2F41").getValue());

    }

}
