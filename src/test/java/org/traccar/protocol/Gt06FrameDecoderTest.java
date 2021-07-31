package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Gt06FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new Gt06FrameDecoder();

        verifyFrame(
                binary("787803691604130318491475905BD30E25001E10BBF7635D14759006E626560501CC0028660F213228660F1F2828660EA81E286610731428660F20140D0A"),
                decoder.decode(null, null, binary("787803691604130318491475905BD30E25001E10BBF7635D14759006E626560501CC0028660F213228660F1F2828660EA81E286610731428660F20140D0A")));

        verifyFrame(
                binary("78780d0103563140414198583c0d0a"),
                decoder.decode(null, null, binary("78780d0103563140414198583c0d0a")));

        verifyFrame(
                binary("787800691709261259400700cc0400d376714600d37a3d5000d37a3c5000d393505a00d3765d5a00d376735a00d32e6b640d0a"),
                decoder.decode(null, null, binary("787800691709261259400700cc0400d376714600d37a3d5000d37a3c5000d393505a00d3765d5a00d376735a00d32e6b640d0a")));

        verifyFrame(
                binary("7878121011091c0b1e2e98058507f80097a6ac03344a0d0a"),
                decoder.decode(null, null, binary("7878121011091c0b1e2e98058507f80097a6ac03344a0d0a")));

        verifyFrame(
                binary("787808171709281135331491827b75594dc8d719a9708452cad719a9708550cad719a97086521491827b75574cac9e17b308085dc8d71939633947cad71939633a480700cc0400d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a0d0a"),
                decoder.decode(null, null, binary("787808171709281135331491827b75594dc8d719a9708452cad719a9708550cad719a97086521491827b75574cac9e17b308085dc8d71939633947cad71939633a480700cc0400d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a00d37a3d5a0d0a")));

        verifyFrame(
                binary("787808134606020002044dc5050d0a"),
                decoder.decode(null, null, binary("787808134606020002044dc5050d0a")));

        verifyFrame(
                binary("78781f1210020e14061dcc0476fcd0003e3faf3e14b20000000000000000044ef6740d0a"),
                decoder.decode(null, null, binary("78781f1210020e14061dcc0476fcd0003e3faf3e14b20000000000000000044ef6740d0a")));

        verifyFrame(
                binary("78780d010352887071911998000479d00d0a"),
                decoder.decode(null, null, binary("78780d010352887071911998000479d00d0a")));

        verifyFrame(
                binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a"),
                decoder.decode(null, null, binary("78782516000000000000c000000000000000000020000900fa0210ef00fb620006640301000468030d0a")));

    }

}
