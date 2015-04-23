package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Xt7ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Xt7ProtocolDecoder decoder = new Xt7ProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "53545832303130313031383031202020202020026A244750524D432C3130313035332E3030302C412C323233322E373630372C4E2C31313430342E373636392C452C302E30302C2C3233313131302C2C2C412A37462C3436302C30302C323739352C304536412C31342C39342C313030302C303030302C39312C54696D65723B31440D0A"))));

    }

}
