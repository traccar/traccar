package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ArknavX8ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        ArknavX8ProtocolDecoder decoder = new ArknavX8ProtocolDecoder(new ArknavX8Protocol());

        verifyNull(decoder, text(
                "351856045213782,241111"));

        verifyNull(decoder, text(
                "2R,090214235955,00,,00.04,03.76,001892024.9"));

        verifyNull(decoder, text(
                "351856040005407,240101"));

        verifyPosition(decoder, text(
                "1R,110509053244,A,2457.9141N,12126.3321E,220.0,315,10.0,00000000"));

        verifyNull(decoder, text(
                "2R,110509053244,837493,,998372,,,"));

        verifyPosition(decoder, text(
                "1G,110509053245,A,2457.9141N,12126.3192E,3.1,35,2.0,00000001"));

        verifyPosition(decoder, text(
                "1G,110509053246,A,2457.9121N,12126.3415E,2.0,288,1.7,00000000"));

        verifyPosition(decoder, text(
                "1M,110509053247,A,2457.9118N,12126.3522E,1.0,55,2.2,00000000"));

    }

}
