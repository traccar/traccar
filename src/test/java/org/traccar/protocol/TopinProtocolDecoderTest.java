package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TopinProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TopinProtocolDecoder decoder = new TopinProtocolDecoder(null);

        verifyNull(decoder, binary(
                "78780d0103593390754169634d0d0a"));

        verifyNotNull(decoder, binary(
                "7878001719111120141807019456465111aa3c465111ab464651c1a550465106b150465342f750465342f65a465111a95a000d0a"));

        verifyPosition(decoder, binary(
                "787812100a03170f32179c026b3f3e0c22ad651f34600d0a"));

        verifyAttributes(decoder, binary(
                "78780713514d0819640d0a"));

    }

}
