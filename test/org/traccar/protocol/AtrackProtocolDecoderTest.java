package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AtrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AtrackProtocolDecoder decoder = new AtrackProtocolDecoder(new AtrackProtocol());

        verifyAttributes(decoder, buffer(
                "$INFO=358683066267395,AX7,Rev.0.61 Build.1624,358683066267395,466924131626767,89886920041316267670,144,0,9,1,12,1,0\r\n"));

        decoder.setLongDate(true);

        verifyPositions(decoder, binary(
                "0203b494003c00eb00014104d8dd3a3e07de011b0b1f0307de011b0b1f0307de011b0b1f0300307f28030574d30000020000000600160100020000000007d007d000"));

        decoder.setLongDate(false);

        decoder.setCustom(true);

        verifyPositions(decoder, binary(
                "405025e30096eb730001452efaf6a7d6562fe4f8562fe4f7562fe52c02a006d902273f810064650000e0f5000a0100000000000007d007d000254349255341254d5625425625475125415400090083002a1a000001a8562fe4f8562fe4f7562fe52c02a006d902273f810064020000e0f5000a0100000000000007d007d000254349255341254d5625425625475125415400090083002a1a000001a8"));

        decoder.setCustom(false);

        verifyNull(decoder, binary(
                "fe0200014104d8f196820001"));

        verifyPositions(decoder, binary(
                "40503835003300070001441c3d8ed1c400000000000000c9000000c900000000000000000000020000000003de0100000000000007d007d000"),
                position("1970-01-01 00:00:00.000", true, 0.00000, 0.00000));

        verifyPositions(decoder, binary(
                "4050993f005c000200014104d8f19682525666c252568c3c52568c63ffc8338402698885000002000009cf03de0100000000000007d007d000525666c252568c5a52568c63ffc8338402698885000002000009cf03de0100000000000007d007d000"));

        verifyPositions(decoder, binary(
                "40501e58003301e000014104d8f19682525ecd5d525ee344525ee35effc88815026ab4d70000020000104403de01000b0000000007d007d000"));

        verifyPositions(decoder, binary(
                "40501e58003301e000014104d8f19682525ecd5d525ee344525ee35effc88815026ab4d70000020000104403de01000b0000000007d007d000000000000000"));

        verifyAttributes(decoder, buffer(
                "$OK\r\n"));

        verifyAttributes(decoder, buffer(
                "$ERROR=101\r\n"));

    }

}
