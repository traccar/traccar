package org.traccar.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObdDecoderTest {
    
    @Test
    public void testDecode() {

        assertEquals(83, ObdDecoder.decode(0x01, "057b").getValue());
        assertEquals(1225, ObdDecoder.decode(0x01, "0C1324").getValue());
        assertEquals(20, ObdDecoder.decode(0x01, "0D14").getValue());
        assertEquals(64050, ObdDecoder.decode(0x01, "31fa32").getValue());
        assertEquals(25, ObdDecoder.decode(0x01, "2F41").getValue());

    }

    @Test
    public void testDecodeCodes() throws Exception {
        assertEquals("P0D14", ObdDecoder.decodeCodes("0D14").getValue());
        assertEquals("dtcs", ObdDecoder.decodeCodes("0D14").getKey());
    }

}
