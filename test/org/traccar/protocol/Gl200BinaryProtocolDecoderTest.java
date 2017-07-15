package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gl200BinaryProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gl200BinaryProtocolDecoder decoder = new Gl200BinaryProtocolDecoder(new Gl200Protocol());

        verifyPosition(decoder, binary(
                "2b4556540c00fc1fbf005c4501010108563254030003430564312a41090100000000003f007dff75a11a025c6a7807e1070a14041202680003189c1ac500000000000000000000000000000000000007e1070b041134054e5c6e0d0a"));

        verifyNull(decoder, binary(
                "2b41434b017f244501010108676231303000000000ffff07e1070b03112d054dfe030d0a"));

    }

}
