package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class CastelProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CastelProtocolDecoder decoder = new CastelProtocolDecoder(null);

        assertNull(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "40401F00043130303131313235323939383700000000000000100303320D0A"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "40407F000431303031313132353239393837000000000000001001C1F06952FDF069529C91110000000000698300000C0000000000036401014C00030001190A0D04121A1480D60488C5721800000000AF4944445F3231364730325F532056312E322E31004944445F3231364730325F482056312E322E31000000DF640D0A"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "40405900043130303131313235323939383700000000000000400101C1F06952E7F069529C9111000000000069830000070000000400036401014C00030001190A0D0412041480D60488C57218000000009F01E803ED9A0D0A"))));

    }

}
