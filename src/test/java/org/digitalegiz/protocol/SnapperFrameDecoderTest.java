package org.digitalegiz.protocol;

import org.junit.jupiter.api.Test;
import org.digitalegiz.ProtocolTest;

public class SnapperFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SnapperFrameDecoder());

        verifyFrame(
                binary("4b0341a6b0c608000040000005000000000000007d5e14010068656c6c6f"),
                decoder.decode(null, null, binary("4b0341a6b0c608000040000005000000000000007d5e14010068656c6c6f")));

        verifyFrame(
                binary("5012"),
                decoder.decode(null, null, binary("5012")));

    }

}
