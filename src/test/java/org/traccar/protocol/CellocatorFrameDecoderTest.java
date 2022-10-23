package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CellocatorFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new CellocatorFrameDecoder());

        verifyFrame(
                binary("4D4347500BA9880B00C80A7E003800000000000806000001210A14000613000000000D4457A5F71442AC02E80300000100040707000011171408060E1C08000100000200020000FD"),
                decoder.decode(null, null, binary("4D4347500BA9880B00C80A7E003800000000000806000001210A14000613000000000D4457A5F71442AC02E80300000100040707000011171408060E1C08000100000200020000FD")));

    }

}
