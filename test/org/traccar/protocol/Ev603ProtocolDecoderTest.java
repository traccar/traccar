package org.traccar.protocol;

import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Ev603ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Ev603ProtocolDecoder decoder = new Ev603ProtocolDecoder(new Ev603Protocol());

        assertNull(decoder.decode(null, null, "!1,123456789012345"));

        assertNull(decoder.decode(null, null, "!5,17,V"));

        assertNull(decoder.decode(null, null, "!1,860719027585011"));

        verify(decoder.decode(null, null,
                "!A,26/10/12,00:28:41,7.770385,-72.215706,0.0,25101,0"));

        verify(decoder.decode(null, null,
                "!A,01/12/10,13:25:35,22.641724,114.023666,000.1,281.6,0"));

        verify(decoder.decode(null, null,
                "!D,08/07/15,04:01:32,40.428257,-3.704808,0,0,170001,701.7,22,5,14,0"));

        verify(decoder.decode(null, null,
                "!D,08/07/15,04:55:13,40.428257,-3.704932,0,0,180001,680.0,8,8,13,0"));

        verify(decoder.decode(null, null,
                "!D,08/07/15,02:01:32,40.428230,-3.704950,4,170,170001,682.7,43,6,13,0"));

    }

}
