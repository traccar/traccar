package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import static org.junit.Assert.assertEquals;

public class AtrackFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AtrackFrameDecoder decoder = new AtrackFrameDecoder();

        assertEquals(
                binary("244F4B0D0A"),
                decoder.decode(null, null, binary("244F4B0D0A")));

        assertEquals(
                binary("fe0200014104d8f196820001"),
                decoder.decode(null, null, binary("fe0200014104d8f196820001")));

        assertEquals(
                binary("40501e58003301e000014104d8f19682525ecd5d525ee344525ee35effc88815026ab4d70000020000104403de01000b0000000007d007d000"),
                decoder.decode(null, null, binary("40501e58003301e000014104d8f19682525ecd5d525ee344525ee35effc88815026ab4d70000020000104403de01000b0000000007d007d000")));

    }

}
