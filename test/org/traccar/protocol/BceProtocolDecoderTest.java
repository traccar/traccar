package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;
import org.traccar.helper.TestDataManager;

public class BceProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        BceProtocolDecoder decoder = new BceProtocolDecoder(new TestDataManager(), null, null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "be76619c834601004200a0003fd769c568ffc3db0079161d420683a9414918b1150000000000d102660167040000000000009f06357f0000a401042ea415e10232000000000000000000000051"))));

    }

}
