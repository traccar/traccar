package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class WatchProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WatchProtocolDecoder decoder = new WatchProtocolDecoder(new WatchProtocol());

        verifyNothing(decoder, text(
                "[SG*8800000015*0002*LK"));

        verifyNothing(decoder, text(
                "[3G*4700186508*000B*LK,0,10,100"));

        verifyPosition(decoder, text(
                "[SG*8800000015*0087*UD,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141"),
                position("2014-04-22 13:46:52.000", true, 22.57171, 113.86140));

        verifyPosition(decoder, text(
                "[SG*8800000015*0087*UD,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141"));

        verifyPosition(decoder, text(
                "[SG*8800000015*0088*UD2,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141"));

        verifyPosition(decoder, text(
                "[SG*8800000015*0087*AL,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0001,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141"));

    }

}
