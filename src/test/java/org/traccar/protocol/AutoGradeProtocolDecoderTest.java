package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class AutoGradeProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AutoGradeProtocolDecoder(null));

        verifyPosition(decoder, text(
                "(000000001637868324027912356171116A2250.7611N07556.9425E000.9024427197.36\u008eA0000B0000C0000D0000E0000K0000L0000M0000N0000O0000)"));

        verifyPosition(decoder, text(
                "(000000007322865733022629240170415A1001.1971N07618.1375E0.000145312128.59?A0024B0024C0000D0000E0000K0000L0000M0000N0000O0000"));

    }

}
