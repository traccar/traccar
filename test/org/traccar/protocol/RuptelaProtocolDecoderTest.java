package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class RuptelaProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        RuptelaProtocolDecoder decoder = new RuptelaProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "00560003116e7438a7a50100015565cbb9000020fd21300f113f4600005f000600090d090805011b13cf00020003001c012029ad00041d31dd1e0ebd160000c50000047200000000d0000000004100016a2a960000a5a300c9ee"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "00a10003116e7438a7a5010002553dddbe000020fddaff0f12289b007200000600000c070805011b18cf00020003001c01201dad01041d32d81e0d7d160000c50000047200000000d000000000410000b1ae960000a5a300553dddd4000020fdd96f0f122bfe005c16f80700050b090805011b18cf00020003001c01201ead01041d338a1e0d8d160000c50000047200000000d000000000410000b1bd960000a5a3001681"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "007900000b1a2a5585c30100024e9c036900000f101733208ff45e07b31b570a001009090605011b1a020003001c01ad01021d338e16000002960000601a41014bc16d004e9c038400000f104fdf20900d20075103b00a001308090605011b1a020003001c01ad01021d33b116000002960000601a41014bc1ea0028f9"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "009200000c07a6bacd4701000552db5cc20000187b8b251ace478e087c044c0a000009070000000052db5cfe0000187b8ab01ace47190879044c0900000b070000000052db5d3a0000187b8b251ace474b089d044c09000009070000000052db5d760000187b8b9a1ace475c08cd044c08000009070000000052db5db20000187b8b141ace46e708b3044c08000009070000000041cb"))));

    }

}
