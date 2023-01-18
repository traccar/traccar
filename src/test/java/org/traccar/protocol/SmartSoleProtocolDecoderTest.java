package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SmartSoleProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SmartSoleProtocolDecoder(null));

        verifyPosition(decoder, text(
                "#GTXRP=359366080000385,8,180514200051,34.041981,-118.255806,60,1,1,7,1.80,180514200051,4.16,11"));

        verifyPosition(decoder, text(
                "#GTXRP=359372090000290,11,180919105751,46.477173,6.445475,389,0,0,0,1.08,180919105751,4.10,10"));

    }

}
