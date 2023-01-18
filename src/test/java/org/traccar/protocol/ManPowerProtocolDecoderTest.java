package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ManPowerProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ManPowerProtocolDecoder(null));

        verifyPosition(decoder, text(
                "simei:352581250259539,,,tracker,51,24,1.73,130426023608,A,3201.5462,N,03452.2975,E,0.01,28B9,1DED,425,01,1x0x0*0x1*60x+2,en-us,"),
                position("2013-04-26 02:36:08.000", true, 32.02577, 34.87163));

        verifyPosition(decoder, text(
                "simei:352581250259539,,,weather,99,20,0.00,130426032310,V,3201.5517,N,03452.3064,E,1.24,28B9,25A1,425,01,1x0x0*0x1*60x+2,en-us,"));
        
        verifyPosition(decoder, text(
                "simei:352581250259539,,,SMS,54,19,90.41,130426172308,V,3201.5523,N,03452.2705,E,0.14,28B9,01A5,425,01,1x0x0*0x1*60x+2,en-us,"));
    }

}
