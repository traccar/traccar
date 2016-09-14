package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AutoGradeProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AutoGradeProtocolDecoder decoder = new AutoGradeProtocolDecoder(new AutoGradeProtocol());

        verifyPosition(decoder, text(
                "(000000007322865733022629240170415A1001.1971N07618.1375E0.000145312128.59?A0024B0024C0000D0000E0000K0000L0000M0000N0000O0000"));

    }

}
