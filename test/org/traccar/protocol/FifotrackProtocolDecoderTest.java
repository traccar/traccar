package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FifotrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FifotrackProtocolDecoder decoder = new FifotrackProtocolDecoder(new FifotrackProtocol());

        verifyPosition(decoder, text(
                "$$135,866104023192332,29,A01,,160606093046,A,22.546430,114.079730,0,186,181,0,415322,0000,02,2,460|0|27B3|EA7,A2F|3B9|3|0,940C7E,31.76|30.98*46"));

    }

}
