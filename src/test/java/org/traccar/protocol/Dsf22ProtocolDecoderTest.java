package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Dsf22ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Dsf22ProtocolDecoder(null));

        verifyPositions(decoder, binary(
                "4642a82d01c8f6aa1af1792c0c1411eb61001e0000"));

        verifyPositions(decoder, binary(
                "4642000101A8EE5F0ECA5FF421B33F524E32610401"));

        verifyPositions(decoder, binary(
                "4642000103A8EE5F0ECA5FF421B33F524E326104010216600EFC92F421B63F524E366104013238600E1EBEF421B93F524E35610401"));

    }

}
