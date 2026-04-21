package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class BwsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new BwsProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "0005e82a1c011e66ed71f7ff813ae0feffb34c0702025d00992a000101c2c800001c1d1617"));

    }

}
