package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FreedomProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FreedomProtocolDecoder(null));

        verifyDecode(decoder, text(
                "IMEI,353358011714362,2014/05/22, 20:49:32, N, Lat:4725.9624, E, Lon:01912.5483, Spd:5.05"),
                position().location("2014-05-22T20:49:32.000Z", true, 47.43271, 19.20914));

        verifyDecode(decoder, text(
                "IMEI,353358011714362,2014/05/22, 20:49:32, N, Lat:4725.9624, E, Lon:01912.5483, Spd:5.05"),
                position());

    }

}
