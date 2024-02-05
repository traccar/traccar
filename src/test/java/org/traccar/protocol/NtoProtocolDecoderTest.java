package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class NtoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NtoProtocolDecoder(null));

        verifyPosition(decoder, text(
                "^NB,880002023090601,N00,050923,233519,V,N,2236.1994,E,11315.4645,5,0,000000000000,460:00:0:75217090,-04:00,,1693971319,31,2DA5"),
                position("2023-09-05 23:35:19.000", false, 22.60332, 113.25774));

    }

}
