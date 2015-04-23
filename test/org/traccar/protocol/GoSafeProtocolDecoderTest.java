package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GoSafeProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GoSafeProtocolDecoder decoder = new GoSafeProtocolDecoder(null);

        assertNull(decoder.decode(null, null, null,
                "*GS16,351535058709775"));

        verify(decoder.decode(null, null, null,
                "*GS16,351535058709775,100356130215,,SYS:G79W;V1.06;V1.0.2,GPS:A;6;N24.802700;E46.616828;0;0;684;1.35,COT:60,ADC:4.31;0.10,DTT:20000;;0;0;0;1"));

    }

}
