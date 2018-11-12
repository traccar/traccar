package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Xrb28ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Xrb28ProtocolDecoder decoder = new Xrb28ProtocolDecoder(null);

        verifyNull(decoder, text(
                "*SCOR,OM,863158022988725,H0,0,412,28,80,0#"));

        verifyNull(decoder, text(
                "*HBCR,OM,123456789123456,Q0,412,80#"));

        verifyNull(decoder, text(
                "*HBCR,OM,123456789123456,R0,0,55,1234,1497689816#"));

        verifyPosition(decoder, text(
                "*HBCR,OM,123456789123456,D0,0,124458.00,A,2237.7514,N,11408.6214,E,6,0.21,151216,10,M,A#"));

        verifyPosition(decoder, text(
                "*SCOR,OM,863158022988725,D0,0,124458.00,A,2237.7514,N,11408.6214,E,6,0.21,151216,10,M,A#"));

    }

}
