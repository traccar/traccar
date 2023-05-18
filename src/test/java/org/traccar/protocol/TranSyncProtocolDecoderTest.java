package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TranSyncProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TranSyncProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "3a3a2b583f086065705154043900801017050b11190f01623ef40887dff00000c25e9ff707000007152a2d0000000105004794916902050000100000050252ee060200822323"));

        verifyAttributes(decoder, binary(
                "3a3a2b583f086065705154043900801017050b11190f01623ef40887dff00000c25e9ff707000007152a2d0000000105004794916902050000000000050252ee060200822323"));

    }

}
