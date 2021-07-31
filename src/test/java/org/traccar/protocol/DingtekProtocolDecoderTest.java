package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class DingtekProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new DingtekProtocolDecoder(null);

        verifyPosition(decoder, text(
                "800001011e0692001a00000000016e008027c40000186962703655111781"));

        verifyNull(decoder, text(
                "8000010333020E180A1E4B1E124801042015693400000000173.249.23.186;6181;159.138.4.6;8888;000000000000000081"));

    }

}
