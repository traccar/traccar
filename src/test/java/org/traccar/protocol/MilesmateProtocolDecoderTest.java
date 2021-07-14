package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MilesmateProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new MilesmateProtocolDecoder(null);

        verifyPosition(decoder, text(
                "ApiString={A:861359037373030,B:09.8,C:00.0,D:083506,E:2838.5529N,F:07717.8049E,G:000.00,H:170918,I:G,J:00004100,K:0000000A,L:1234,M:126.86}"));

        verifyPosition(decoder, text(
                "ApiString={A:861359037496211,B:12.7,C:06.0,D:060218,E:2837.1003N,F:07723.3162E,G:016.80,H:310818,I:G,J:10010100,K:0000000A,L:1234,M:358.33}"),
                position("2018-08-31 06:02:18.000", true, 28.61834, 77.38860));

        verifyPosition(decoder, text(
                "ApiString={A:862631032208018,B:12.1,C:24.4,D:055852,E:2838.5310N,F:07717.8126E,G:000.0,H:200117,I:G,J:10100100,K:1000000A,L:1234,M:324.45}"));

    }

}
