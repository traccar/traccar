package org.traccar.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObdDecoderTest {
    
    @Test
    public void testDecode() {

        assertEquals(83L, ObdDecoder.decode(0x01, "057b").getValue());
        assertEquals(1225L, ObdDecoder.decode(0x01, "0C1324").getValue());
        assertEquals(20L, ObdDecoder.decode(0x01, "0D14").getValue());
        assertEquals(64050L, ObdDecoder.decode(0x01, "31fa32").getValue());
        assertEquals(25L, ObdDecoder.decode(0x01, "2F41").getValue());

    }

    @Test
    public void testDecodeCodes() throws Exception {
        assertEquals("P0D14", ObdDecoder.decodeCodes("0D14").getValue());
        assertEquals("dtcs", ObdDecoder.decodeCodes("0D14").getKey());
    }

}
