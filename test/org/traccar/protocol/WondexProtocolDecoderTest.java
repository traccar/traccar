package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class WondexProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WondexProtocolDecoder decoder = new WondexProtocolDecoder(null);
        
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
        
        verify(decoder.decode(null, null,
                "3997324533,20140326074908,28.797603,47.041635,0,48,0,6,2,3.90V,0"));
        
        verify(decoder.decode(null, null,
                "2000000001,20140529213210,-63.179111,9.781493,0,0,54.0,8,2,0.0,0,0.01,0.01,0,0,0,0"));

    }

}
