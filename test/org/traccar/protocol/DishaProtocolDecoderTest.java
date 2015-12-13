package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class DishaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        DishaProtocolDecoder decoder = new DishaProtocolDecoder(new DishaProtocol());

        verifyPosition(decoder, text(
                "$A#A#353943046615971#A#064219#281113#1836.7267#N#07347.4177#E#000.00#280.4#09#00.8#3000#2#100#0000#8888#86.5#3919.1#0000*"));

    }

}
