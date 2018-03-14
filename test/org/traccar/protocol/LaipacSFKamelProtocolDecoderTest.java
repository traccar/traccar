package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class LaipacSFKamelProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        LaipacSFKamelProtocolDecoder decoder = new LaipacSFKamelProtocolDecoder(new LaipacSFKamelProtocol());

        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,084514,r,5050.1314,N,00419.9719,E,0.68,306.39,120318,0,3882,84,1,0,0,3EE4A617,020610*4E"));

        verifyNull(decoder, text(
                "$AVSYS,99999999,V1.50,SN0000103,32768*15"));
        
        verifyNull(decoder, text(
                "$ECHK,99999999,0*35"));
        
        verifyNull(decoder, text(
                "$AVSYS,MSG00002,14406,7046811160,64*1A"));

        verifyNull(decoder, text(
                "$EAVSYS,MSG00002,8931086013104404999,,Owner,0x52014406*76"));

        verifyNull(decoder, text(
                "$ECHK,MSG00002,0*5E"));

        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,111602,r,5050.1262,N,00419.9660,E,0.00,0.00,120318,0,3843,95,1,0,0,3EE4A617,020610*44"));
    }

}
