package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NvsFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new NvsFrameDecoder());

        assertEquals(
                binary("0012333537303430303630303137383234312e38"),
                decoder.decode(null, null, binary("0012333537303430303630303137383234312e38")));

        assertEquals(
                binary("cccccccc0073000144b9ddf2aca002015694823d1f165d80902139a44f00aa001e1400000103000a080115001a001d001e0141004001f00065001301061600001700001800004231da430000440000085000000000480000000049000000004a0000000047ffffffff6900000004c700000000e10000000100954a"),
                decoder.decode(null, null, binary("cccccccc0073000144b9ddf2aca002015694823d1f165d80902139a44f00aa001e1400000103000a080115001a001d001e0141004001f00065001301061600001700001800004231da430000440000085000000000480000000049000000004a0000000047ffffffff6900000004c700000000e10000000100954a")));

    }

}
