package org.traccar.protocol;

import org.junit.Test;

public class TytanProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TytanProtocolDecoder decoder = new TytanProtocolDecoder(new TytanProtocol());
        
        /*verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "d5300000689d65556877fbd437a09d8ea89360005a23124e410201213704000907000009ffffffffffffffff0affffffffffffffff18ff63ff7f7fff65ff66ff67ff68ff69ff6b00ff6cffffffff6dff7fffffffff81ffffffff82ffff83ffffffffffffffff88ffff9600"))));*/

    }

}
