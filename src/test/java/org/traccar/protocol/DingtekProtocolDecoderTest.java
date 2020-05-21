package org.traccar.protocol;

import org.junit.Ignore;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class DingtekProtocolDecoderTest extends ProtocolTest {

    @Ignore
    @Test
    public void testDecode() throws Exception {

        DingtekProtocolDecoder decoder = new DingtekProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "800001011e0692001a00000000016e008027c40000186962703655111781"));

    }

}
