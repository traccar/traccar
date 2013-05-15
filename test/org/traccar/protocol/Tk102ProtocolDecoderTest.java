package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Tk102ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tk102ProtocolDecoder decoder = new Tk102ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, ""));

        assertNull(decoder.decode(null, null,
                "[!0000000081r(353327023367238,TK102-W998_01_V1.1.001_130219,255,001,255,001,0,100,100,0,internet,0000,0000,0,0,255,0,4,1,11,00)"));

        assertNull(decoder.decode(null, null,
                "[%0000000082\u001d(100100000000000600-30-65535)"));

        assertNotNull(decoder.decode(null, null,
                "[=00000000836(ITV013939A4913.8317N02824.9241E000.90018031310010000)"));
        
        assertNotNull(decoder.decode(null, null,
                "[=00000000366(ITV012209A4913.8281N02824.9258E000.32018031310010000)"));

    }

}
