package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gt30ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gt30ProtocolDecoder decoder = new Gt30ProtocolDecoder(new Gt30Protocol());

        verifyPosition(decoder, text(
                "$$005B3037811124    9955161049.000,A,3802.9474,N,02241.1897,E,0.00,,021115,,*15|2.9|5A639"));

    }

}
