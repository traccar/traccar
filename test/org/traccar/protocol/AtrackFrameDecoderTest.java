package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import static org.junit.Assert.assertEquals;

public class AtrackFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AtrackFrameDecoder decoder = new AtrackFrameDecoder();

        assertEquals(
                binary("40502c373542332c3132302c37393737392c3335383930313034383039313535342c32303138303431323134323531342c32303138303431323134323531342c32303138303431333030303635352c31363432333338392c34383137383730302c3130382c322c362e352c392c302c302c302c302c302c323030302c323030302c1a0d0a"),
                decoder.decode(null, null, binary("40502c373542332c3132302c37393737392c3335383930313034383039313535342c32303138303431323134323531342c32303138303431323134323531342c32303138303431333030303635352c31363432333338392c34383137383730302c3130382c322c362e352c392c302c302c302c302c302c323030302c323030302c1a0d0a")));

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
