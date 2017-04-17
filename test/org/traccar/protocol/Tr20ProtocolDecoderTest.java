package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Tr20ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Tr20ProtocolDecoder decoder = new Tr20ProtocolDecoder(new Tr20Protocol());

        verifyNull(decoder, text(
                "%%TRACKPRO01,1"));

        verifyPosition(decoder, text(
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"),
                position("2005-09-16 07:05:49.000", true, 22.68148, 113.98832));

        verifyPosition(decoder, text(
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"));

    }

}
