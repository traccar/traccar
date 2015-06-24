package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Ev603ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Ev603ProtocolDecoder decoder = new Ev603ProtocolDecoder(new Ev603Protocol());

        assertNull(decoder.decode(null, null, "!1,123456789012345"));

        assertNull(decoder.decode(null, null, "!5,17,V"));

        verify(decoder.decode(null, null,
                "!A,26/10/12,00:28:41,7.770385,-72.215706,0.0,25101,0"));

        verify(decoder.decode(null, null,
                "!A,01/12/10,13:25:35,22.641724,114.023666,000.1,281.6,0"));

    }

}
