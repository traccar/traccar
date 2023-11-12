package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PositrexProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PositrexProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "8280902e002b81c99fd607033905008b1c000003ae00003c9c000054ee00000079000000000d34d43f0fffffffda0000000000104fb80000204086464717807f8931082622128190980fffff862261047296590fffff"));

    }

}
