package org.traccar.protocol;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PositrexProtocolDecoderTest extends ProtocolTest {

    @Disabled
    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PositrexProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "TODO"));

    }

}
