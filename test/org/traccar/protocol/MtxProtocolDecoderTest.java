package org.traccar.protocol;

import static org.traccar.helper.DecoderVerifier.verify;

import org.junit.Test;

public class MtxProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MtxProtocolDecoder decoder = new MtxProtocolDecoder(new MtxProtocol());

        verify(decoder.decode(null, null,
                "#MTX,353815011138124,20101226,195550,41.6296399,002.3611174,000,035,000000.00,X,X,1111,000,0,0"));

    }

}
