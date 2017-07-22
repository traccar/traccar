package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gl200BinaryProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Gl200BinaryProtocolDecoder decoder = new Gl200BinaryProtocolDecoder(new Gl200Protocol());

        verifyAttributes(decoder, binary(
                "2b494e4601fd7f0076676231303000000045010202090104020500004100054007e107150b061d0000003f010e02580000000000d0312a1013648935103226313921591f1200000000000302680003189c1ac3001b02680003189c1ac4000d02680003189c1ac5001207e107150b0d3704f658060d0a"));

        verifyPosition(decoder, binary(
                "2b4556540c00fc1fbf005c4501010108563254030003430564312a41090100000000003f007dff75a11a025c6a7807e1070a14041202680003189c1ac500000000000000000000000000000000000007e1070b041134054e5c6e0d0a"));

        verifyNull(decoder, binary(
                "2b41434b017f244501010108676231303000000000ffff07e1070b03112d054dfe030d0a"));

    }

}
