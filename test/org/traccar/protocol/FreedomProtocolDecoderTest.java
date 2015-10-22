package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class FreedomProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        FreedomProtocolDecoder decoder = new FreedomProtocolDecoder(new FreedomProtocol());

        verifyPosition(decoder, text(
                "IMEI,353358011714362,2014/05/22, 20:49:32, N, Lat:4725.9624, E, Lon:01912.5483, Spd:5.05"));
        
    }

}
