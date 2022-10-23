package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Pt215ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Pt215ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "58580d010359339075435451010d0a"));

        verifyNull(decoder, binary(
                "585801080d0a"));

    }

}
