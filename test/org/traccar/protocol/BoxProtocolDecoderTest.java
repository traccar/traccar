package org.traccar.protocol;


import org.junit.Test;
import org.traccar.ProtocolTest;

public class BoxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        BoxProtocolDecoder decoder = new BoxProtocolDecoder(new BoxProtocol());
        
        verifyNothing(decoder, text(
                "H,BT,358281002435893,081028142432,F5813D19,6D6E6DC2"));
        
        verifyNothing(decoder, text(
                "H,BT,N878123,080415081234,D63E6DD9,6D6E6DC2,8944100300825505377"));

        verifyPosition(decoder, text(
                "L,081028142429,G,52.51084,-1.70849,0,170,0,1,0"));

        verifyPosition(decoder, text(
                "L,081028142432,G,52.51081,-1.70849,0,203,0,16,0"));

        verifyNothing(decoder, text(
                "L,080528112501,AI1,145.56"));

        verifyNothing(decoder, text(
                "E,1"));

        verifyPosition(decoder, text(
                "L,150728150130,G,24.68312,46.67526,0,140,0,3,20;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728155815,G,24.68311,46.67528,0,140,0,6,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728155833,G,24.68311,46.67528,11,140,0,52,23;A,0.79;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728155934,G,24.68396,46.67489,0,282,0.12,1,21;A,1.27;D,1.23;I,0"));

        verifyPosition(decoder, text(
                "L,150728160033,G,24.68414,46.67485,0,282,0.12,1,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728160133,G,24.68388,46.675,0,282,0.12,1,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728160233,G,24.68377,46.67501,0,282,0.12,1,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728160333,G,24.684,46.67488,0,282,0.12,1,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728155855,G,24.68413,46.67482,0,282,0.14,53,21;A,0;D,0;I,0"));

        verifyPosition(decoder, text(
                "L,150728160400,G,24.68413,46.67482,0,282,0.14,7,20;A,0;D,0;I,0;END,25,326,150728155814"));

    }

}
