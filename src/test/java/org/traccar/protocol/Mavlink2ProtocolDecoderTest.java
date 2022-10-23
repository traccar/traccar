package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Mavlink2ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Mavlink2ProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "fd1c0000ce01012100004da91f004005d323b89aa30ea6ed070099fb0100f7fffdff0000942c4a88"));

        verifyAttributes(decoder, binary(
                "fd1c0000e7010121000047aa1f004005d323b89aa30e9ced070093fb0100f8fffdff0000952c70ff"));

    }

}
