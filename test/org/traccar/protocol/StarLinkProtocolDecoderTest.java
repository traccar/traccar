package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class StarLinkProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        StarLinkProtocolDecoder decoder = new StarLinkProtocolDecoder(new StarLinkProtocol());

        verifyNothing(decoder, text(
                "$SLU005F20,06,22743,170116091944,01,170116091944,+3206.0991,+03452.0605,003.6,008,064675,1,1,0,0,0,0,0,0,10424,2521,14.156,01.163,,1,1,1,4*BE"));

        verifyNothing(decoder, text(
                "$SLU005F20,06,22718,170116091422,01,170116091422,+3205.1777,+03450.7595,046.8,359,064671,1,1,0,0,0,0,0,0,10424,64072,14.148,01.161,,1,1,1,4*03"));

        verifyNothing(decoder, text(
                "$SLU005F20,06,22695,170116090730,24,170116090730,+3203.6062,+03449.6945,013.9,181,064666,1,1,0,0,0,0,0,0,10422,30631,14.089,01.163,,1,1*43"));

        verifyPosition(decoder, text(
                "$SLU0004D2,06,32,071106135931,01,071106135930,+3159.4376,+03445.3298,021.3,087,000554,31071,11704,13.45,3.87*3E"));

    }

}
