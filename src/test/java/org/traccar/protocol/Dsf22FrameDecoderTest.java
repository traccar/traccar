package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Dsf22FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Dsf22FrameDecoder());

        verifyFrame(
                binary("4642000101A8EE5F0ECA5FF421B33F524E32610401"),
                decoder.decode(null, null, binary("4642000101A8EE5F0ECA5FF421B33F524E32610401")));

        verifyFrame(
                binary("4642000103A8EE5F0ECA5FF421B33F524E326104010216600EFC92F421B63F524E366104013238600E1EBEF421B93F524E35610401"),
                decoder.decode(null, null, binary("4642000103A8EE5F0ECA5FF421B33F524E326104010216600EFC92F421B63F524E366104013238600E1EBEF421B93F524E35610401")));

    }

}
