package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Tr20ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tr20ProtocolDecoder decoder = new Tr20ProtocolDecoder(new Tr20Protocol());

        assertNull(decoder.decode(null, null, "%%TRACKPRO01,1"));

        verify(decoder.decode(null, null,
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"));

    }

}
