package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProgressProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        ProgressProtocolDecoder decoder = new ProgressProtocolDecoder(new ProgressProtocol());

        verifyNull(decoder, binary(
                "020037000100000003003131310f003335343836383035313339303036320f00323530303136333832383531353535010000000100000000000000e6bb97b6"));

    }

    @Test
    public void testDecodeShouldThrowAnException() throws Exception {
        try {
            binary("++");
            fail("should have thrown RuntimeException");
        } catch (RuntimeException expected) {
            assertEquals("org.apache.commons.codec.DecoderException: Illegal hexadecimal character + at index 0", expected.getMessage());
        }
    }

}
