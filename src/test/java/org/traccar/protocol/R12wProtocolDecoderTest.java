package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class R12wProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new R12wProtocolDecoder(null);

        verifyNull(decoder, text(
                "$HX,0001,860721009104316,e92c,933402042499509,55792760080,12345678,01,a8d940a9,#,50,"));

    }

}
