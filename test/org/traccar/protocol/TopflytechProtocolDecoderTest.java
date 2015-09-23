package org.traccar.protocol;

import static org.traccar.helper.DecoderVerifier.verify;

import org.junit.Test;

public class TopflytechProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TopflytechProtocolDecoder decoder = new TopflytechProtocolDecoder(new TopflytechProtocol());

        verify(decoder.decode(null, null,
                "(880316890094910BP00XG00b600000000L00074b54S00000000R0C0F0014000100f0130531152205A0706.1395S11024.0965E000.0251.25"));

    }

}
