package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class MeitrackProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MeitrackProtocolDecoder decoder = new MeitrackProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNotNull(decoder.decode(null, null,
                "$$X138,862170010187175,AAA,35,-29.960365,-51.655455,130507201625,A,8,9,0,107,0.9,7,169322,126582,724|6|0547|132B,0000,0009|000A||0278|0000,*BE"));

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
        
        assertNotNull(decoder.decode(null, null,
                "$$_128,861074020109479,AAA,34,22.512618,114.057065,090215000318,V,0,31,0,0,0,0,0,733,302|720|3EE4|BBB5,0000,0006|0006||028C|0000,*E3"));
        
        assertNotNull(decoder.decode(null, null,
                "$$K146,013227004985762,AAA,35,28.618005,-81.246783,131101213828,A,9,22,0,209,1.1,23,80974,1187923,310|260|2A13|634E,0000,0000|0000|0000|09DA|0B34,,*51"));

    }

}
