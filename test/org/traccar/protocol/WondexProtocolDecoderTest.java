package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class WondexProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WondexProtocolDecoder decoder = new WondexProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        verify(decoder.decode(null, null,
                "1044989601,20130323074605,0.000000,90.000000,0,000,0,0,2"));

        verify(decoder.decode(null, null,
                "123456789000001,20120101123200,130.000000,60.000000,0,000,0,0,0,0"));

        verify(decoder.decode(null, null,
                "210000001,20070313170040,121.123456,12.654321,0,233,0,9,2,0.0,0,0.00,0.00,0"));

        verify(decoder.decode(null, null,
                "1044989601,20130322172647,13.572583,52.401070,22,204,49,0,2"));

        verify(decoder.decode(null, null,
                "1044989601,20130322172647,13.572583,52.401070,22,204,-49,0,2"));

    }

}
