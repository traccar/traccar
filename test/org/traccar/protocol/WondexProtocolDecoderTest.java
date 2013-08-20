package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class WondexProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WondexProtocolDecoder decoder = new WondexProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNotNull(decoder.decode(null, null,
                "1044989601,20130323074605,0.000000,90.000000,0,000,0,0,2"));

        assertNotNull(decoder.decode(null, null,
                "123456789000001,20120101123200,130.000000,60.000000,0,000,0,0,0,0"));

        assertNotNull(decoder.decode(null, null,
                "210000001,20070313170040,121.123456,12.654321,0,233,0,9,2,0.0,0,0.00,0.00,0"));

        assertNotNull(decoder.decode(null, null,
                "1044989601,20130322172647,13.572583,52.401070,22,204,49,0,2"));

    }

}
