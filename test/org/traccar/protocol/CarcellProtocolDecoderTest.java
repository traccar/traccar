package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CarcellProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeCR250() throws Exception {
        
        CarcellProtocolDecoder decoder = new CarcellProtocolDecoder(new CarcellProtocol());
        
        verifyPosition(decoder, text(
                "$863071010274949,S2331.4774,W04629.5123,000,194,0,01,16,0,01,G,230715,115017,0,0,01,A0,1246,89551093619008954621,1D\r\n"));
        
        verifyPosition(decoder, text(
                "$863071010274949,CEL,S23.5257,W046.4953,000,000,0,01,16,0,00,C,230715,114612,0,0,01,A0,1246,89551093619008954621,5E\r\n"));
    }
    
    @Test
    public void testDecodeCR2000() throws Exception {
        
        CarcellProtocolDecoder decoder = new CarcellProtocolDecoder(new CarcellProtocol());

        verifyPosition(decoder, text(
                "$012207003514418,S1234.1423,W15045.1846,044,289,+025+120-120,48,14,0,02,G140211,185104,0,1,0,A0,1061,4A\r\n"),
                position("2011-02-14 18:51:04.000", true, -12.569038, -150.753076));

        verifyPosition(decoder, text(
                "$012207003514418,S1234.1649,W15045.2298,092,289,+000+000+000,48,14,0,02,C010813,125423,0,1,0,A0,1061,46\r\n"));
        
        verifyPosition(decoder, text(
                "$012207003514418,S1234.1137,W15045.1274,255,289,+000+000+000,47,14,0,02,G140211,184618,0,1,0,A0,1061,46\r\n"));
        
        verifyPosition(decoder, text(
                "%012207003514418,S1234.0658,W15045.0316,109,289,-000+000+000,29,14,0,02,G140211,183819,0,1,0,A0,1061,46\r\n"));
        
        verifyPosition(decoder, text(
                "%012207003514418,S1234.0712,W15045.0424,209,289,+080-127-005,29,14,0,02,G140211,183913,0,1,0,A0,1061,4E\r\n"));
        
        verifyPosition(decoder, text(
                "%012207003514418,S1234.0806,W15045.0612,012,289,+090+100+110,47,14,0,02,G140211,184047,0,1,0,A0,1061,4D\r\n"));
        
        verifyPosition(decoder, text(
                "%012207003514418,S1234.1003,W15045.1006,007,289,+000+000+000,47,14,0,02,G140211,184404,0,1,0,A0,1061,4C\r\n"));
        
        verifyPosition(decoder, text(
                "%012207003514418,S1234.1064,W15045.1128,120,289,-125-115-105,47,14,0,02,G140211,184505,0,1,0,A0,1061,45\r\n"));
        
        verifyPosition(decoder, text(
                "$860058010071287,CEL,S23.5445,W046.5896,000,000,+000+000+000,00,10,0,00,C120413,091205,0,0,0,A1,1217,05\r\n"));
        
        verifyPosition(decoder, text(
                "$860058010071287,CEL,S23.5445,W046.5896,000,000,+000+000+000,00,10,0,00,C120413,091205,0,0,0,F1,1217,05\r\n"));
        
        verifyPosition(decoder, text(
                "$860058010071287,CEL,S23.5445,W046.5896,000,000,+000+000+000,00,10,0,00,C120413,091205,0,0,0,F2,1217,05\r\n"));
        
        verifyPosition(decoder, text(
                "$860058010071287,CEL,S23.5445,W046.5896,000,000,+000+000+000,00,10,0,00,C120413,091205,0,0,0,F3,1217,05\r\n"));
        
        verifyNothing(decoder, text(
                "$CLNACK,012207003514418,BA#"));
        
        verifyNothing(decoder, text(
                "$CLNACK,012207003514418,BSA#"));
        
        verifyNothing(decoder, text(
                "$CLNACK,012207003514418,BD#"));
        
        verifyNothing(decoder, text(
                "$CLNACK,012207003514418,AP#"));
        

    }

}
