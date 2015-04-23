package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class AplicomProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        AplicomProtocolDecoder decoder = new AplicomProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "44C20143720729D6840043031fff7191C0450ef906450ef90603b20b8003b20b80066465b3870ce30f010ce30ce3003200001520000000030aa200003b13000000320300000bcb17acff0099000186a002"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "440129D684002b0700C0450ef906450ef90603b20b8003b20b80066465b3870ce30f010ce30ce300003b130300000bcb170a"))));

    }

}
