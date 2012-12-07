package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class MeitrackProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MeitrackProtocolDecoder decoder = new MeitrackProtocolDecoder(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "$$]138,012896000475498,AAA,35,-6.138255,106.910545,121205074600,A,5,18,0,0,0,49,3800,24826,510|10|0081|4F4F,0000,0011|0012|0010|0963|0000,,*94"));

        assertNotNull(decoder.decode(null, null,
                "$$d138,012896000475498,AAA,35,-6.138255,106.910545,121205074819,A,7,18,0,0,0,49,3800,24965,510|10|0081|4F4F,0000,000D|0010|0012|0963|0000,,*BF"));

        assertNotNull(decoder.decode(null, null,
                "$$j138,012896000475498,AAA,35,-6.138306,106.910655,121205103708,A,3,11,0,0,1,36,4182,35025,510|10|0081|4F4F,0000,000A|000C|000A|0915|0000,,*BF"));
        
    }

}
