package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xrb28ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xrb28ProtocolDecoder(null));

        verifyAttributes(decoder, text(
                "*SCOR,OM,123456789123456,Q0,412,80,28#"));

        verifyPosition(decoder, text(
                "*SCOR,OM,867584030387299,D0,0,012102.00,A,0608.00062,S,10659.70331,E,12,0.69,151118,30.3,M,A#"));

        verifyAttributes(decoder, text(
                "*SCOR,OM,863158022988725,H0,0,412,28,80,0#"));

        verifyAttributes(decoder, text(
                "*HBCR,OM,123456789123456,R0,0,55,1234,1497689816#"));

        verifyPosition(decoder, text(
                "*HBCR,OM,123456789123456,D0,0,124458.00,A,2237.7514,N,11408.6214,E,6,0.21,151216,10,M,A#"));

        verifyPosition(decoder, text(
                "*SCOR,OM,863158022988725,D0,0,124458.00,A,2237.7514,N,11408.6214,E,6,0.21,151216,10,M,A#"));
        
        verifyPosition(decoder, text(
                "*SCOR,NG,868020030308430,D0,1,020455.00,A,2359.36129,S,04615.24677,W,12,0.72,201119,8.5,M,A#"));

    }

}
