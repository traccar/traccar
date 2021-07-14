package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SmokeyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new SmokeyProtocolDecoder(null);

        verifyAttributes(decoder, binary(
                "534d0300865101019383025f0403000000000b86250200000c0000028f000102f8cc0900127f08"));

        verifyAttributes(decoder, binary(
                "534d0300865101019383025f0403000000000bcf260200000c0000028f000102f8cc090012360b"));

    }

}
