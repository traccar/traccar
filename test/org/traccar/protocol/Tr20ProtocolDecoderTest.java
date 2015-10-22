package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class Tr20ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tr20ProtocolDecoder decoder = new Tr20ProtocolDecoder(new Tr20Protocol());

        verifyNothing(decoder, text( "%%TRACKPRO01,1"));

        verifyPosition(decoder, text(
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"));

    }

}
