package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PstFrameEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var channel = channel(new PstFrameEncoder());

        verifyEncode(channel, binary("2FAF0B10059A0000B001022FAF0B10E91349A2AD3B1DAD2FF8A78228A58F"),
                binary("282FAF0B10059A0000B001022FAF0B10E91349A2AD3B1DAD2FF8A7822768A58F29"));

    }

}
