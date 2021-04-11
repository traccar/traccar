package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TrakMateProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new TrakMateProtocolDecoder(null);

        verifyPosition(decoder, text(
                "^TMSTP|352984083995323|116|13.07809|77.55979|131508|131118|0.0|146.51|7|0|71 -2 248|0|13.1|0.0|10.5|1|0|0|0|#"));

        verifyPosition(decoder, text(
                "^TMPER|354678456723764|1|12.59675|77.56789|123456|030414|2.3|34.0|1|0|0|0.015|3.9|12.0|23.4|23.4|1|1|0|#"));

        verifyPosition(decoder, text(
                "^TMALT|354678456723764|3|2|1|12.59675|77.56789|123456|030414|1.2|34.0|#"));

        verifyPosition(decoder, text(
                "^TMSRT|354678456723764|12.59675|77.56789|123456|030414|1.03|1.01|#"));

    }

}
