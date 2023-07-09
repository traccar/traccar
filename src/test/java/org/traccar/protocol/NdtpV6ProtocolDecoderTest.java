package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class NdtpV6ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NdtpV6ProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "7e7e3b000200334202000000000000000064000100000000000600020002034f0c0200000400000000000033353135313330353131393430353532353030323632373237343836363500"));

    }

}
