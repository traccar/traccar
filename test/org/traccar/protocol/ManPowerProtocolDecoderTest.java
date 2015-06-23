package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class ManPowerProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        ManPowerProtocolDecoder decoder = new ManPowerProtocolDecoder(new ManPowerProtocol());

        verify(decoder.decode(null, null,
                "simei:352581250259539,,,tracker,51,24,1.73,130426023608,A,3201.5462,N,03452.2975,E,0.01,28B9,1DED,425,01,1x0x0*0x1*60x+2,en-us,"));

        verify(decoder.decode(null, null,
                "simei:352581250259539,,,weather,99,20,0.00,130426032310,V,3201.5517,N,03452.3064,E,1.24,28B9,25A1,425,01,1x0x0*0x1*60x+2,en-us,"));
        
        verify(decoder.decode(null, null,
                "simei:352581250259539,,,SMS,54,19,90.41,130426172308,V,3201.5523,N,03452.2705,E,0.14,28B9,01A5,425,01,1x0x0*0x1*60x+2,en-us,"));
    }

}
