package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class UlbotechFrameDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        UlbotechFrameDecoder decoder = new UlbotechFrameDecoder();

        Assert.assertEquals(
                binary("f8010108679650230646339de69054010e015ee17506bde2c60000000000ac0304024000000404000009f705060390181422170711310583410c0000310d00312f834131018608040003130a100101136cf8"),
                decoder.decode(null, null, binary("f8010108679650230646339de69054010e015ee17506bde2c60000000000ac0304024000000404000009f70005060390181422170711310583410c0000310d00312f834131018608040003130a100101136cf8")));

    }

}
