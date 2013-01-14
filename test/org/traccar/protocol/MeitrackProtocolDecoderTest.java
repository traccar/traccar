package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class MeitrackProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MeitrackProtocolDecoder decoder = new MeitrackProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "$$]138,012896000475498,AAA,35,-6.138255,106.910545,121205074600,A,5,18,0,0,0,49,3800,24826,510|10|0081|4F4F,0000,0011|0012|0010|0963|0000,,*94"));

        assertNotNull(decoder.decode(null, null,
                "$$d138,012896000475498,AAA,35,-6.138255,106.910545,121205074819,A,7,18,0,0,0,49,3800,24965,510|10|0081|4F4F,0000,000D|0010|0012|0963|0000,,*BF"));

        assertNotNull(decoder.decode(null, null,
                "$$j138,012896000475498,AAA,35,-6.138306,106.910655,121205103708,A,3,11,0,0,1,36,4182,35025,510|10|0081|4F4F,0000,000A|000C|000A|0915|0000,,*BF"));

        assertNotNull(decoder.decode(null, null,
                "$$m139,012896005334567,AAA,35,-33.866423,151.190060,121208020649,A,7,27,0,32,4,13,6150,49517,505|2|0B67|5A6C,0000,0000|0000|0000|0977|0000,,*F1"));

        assertNotNull(decoder.decode(null, null,
                "$$A141,012896005334567,AAA,35,-33.866543,151.190148,121209081758,A,6,27,0,16,1,48,65551,152784,505|2|0B5F|D9D3,0000,0000|0000|0000|0A39|0000,,*5B"));

    }

}
