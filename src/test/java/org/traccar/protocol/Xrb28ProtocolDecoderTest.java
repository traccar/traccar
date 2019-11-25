package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Xrb28ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Xrb28ProtocolDecoder decoder = new Xrb28ProtocolDecoder(null);

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
                "*SCOR,NG,868020030346166,D0,1,020047.000,A,2359.35484,S,04615.26737,W,14,0.77,161119,-11.87,M,A#"));
        
        verifyPosition(decoder, text(
                "*SCOR,NG,868020030485741,D0,1,020249.000,A,2359.36260,S,04615.24544,W,13,0.77,201119,13.52,M,A#"));
        
        verifyPosition(decoder, text(
                "*SCOR,NG,868020030491178,D0,1,020533.000,A,2359.34836,S,04615.24559,W,16,0.68,111119,40.97,M,A#"));
        
        verifyPosition(decoder, text(
                "*SCOR,NG,868020030432826,D0,1,020522.000,A,2359.36061,S,04615.24528,W,14,0.76,191119,16.50,M,A#"));
        
        verifyPosition(decoder, text(
                "*SCOR,NG,868020030308430,D0,1,020455.00,A,2359.36129,S,04615.24677,W,12,0.72,201119,8.5,M,A#"));

        verifyPosition(decoder, text(
                "*SCOR,NG,868020030485741,D0,1,020519.000,A,2359.36247,S,04615.24459,W,14,0.74,201119,13.56,M,A#"));

    }

}
