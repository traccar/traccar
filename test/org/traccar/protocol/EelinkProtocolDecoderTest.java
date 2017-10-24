package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EelinkProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        EelinkProtocolDecoder decoder = new EelinkProtocolDecoder(new EelinkProtocol());

        verifyNull(decoder, binary(
                "676701000c007b03525440717505180104"));

        verifyPosition(decoder, binary(
                "6767120048000559c1829213059a7400008e277d000c000000000800cc00080d2a000034df3cf0b429dd82cad3048910320000000000007b7320d005ba0000000019a000000000000000000000"));

        verifyPosition(decoder, binary(
                "6767050020213b59c6aecdff41dce70b8b977d00000001fe000a36e30078fe010159c6aecd"));

        verifyPosition(decoder, binary(
                "676705002102b459ae7388fcd360d7034332b1000000028f000a4f64002eb101010159ae7388"));

        verifyPosition(decoder, binary(
                "676702001c02b259ae7387fcd360d6034332b2000000028f000a4f64002eb10101"));

        verifyPosition(decoder, binary(
                "6767050022001F59643640000000000000000000000001CC0000249500142000015964A6C0006E"));

        verifyAttributes(decoder, binary(
                "67670300040021006E"));

        verifyPosition(decoder, binary(
                "676705002200255964369D000000000000000000000001CC0000249500142000025964A71D006A"));

        verifyAttributes(decoder, binary(
                "67670300040028006A"));

        verifyPosition(decoder, binary(
                "676712002d066c592cca6803002631a60b22127700240046005c08020d000301af000da0fd12007f11ce05820000001899c0"));

        verifyPosition(decoder, binary(
                "676702002509f65868507603a1e92e03cf90fe000000019f000117ee00111e0120631145003101510000"));

        verifyPosition(decoder, binary(
                "676712001e0092579714d60201f90001785003a301cd1a006a118504f2000000000000"));

        verifyPosition(decoder, binary(
                "676712003400505784cc0b130246479b07d05a06001800000000070195039f046100002cc52e6466b391604a4900890e7c00000000000006ca"));

        verifyPosition(decoder, binary(
                "676714002b00515784cc24130246479b07d05a06001800010000060195039f046100002cc52f6466b391604a49020089"));

        verifyNull(decoder, binary(
                "676701000c002603541880486128290120"));

        verifyPosition(decoder, binary(
                "676704001c01a4569ff2dd0517a0f7020b0d9a06011000d8001e005b0004450183"));

        verifyPosition(decoder, binary(
                "676705002200ba569fc3520517a0d8020b0f740f007100d8001e005b0004460101569fd162001f"));

        verifyPosition(decoder, binary(
                "676702002500bb569fc3610517a091020b116000001900d8001e005b00044601001f1170003200000000"));

        verifyPosition(decoder, binary(
                "676704001c00b7569fc3020517a2d7020b08e100000000d8001e005b0004460004"));

        verifyNull(decoder, binary(
                "676701000b001b035418804661834901"));

        verifyAttributes(decoder, binary(
                "6767030004001A0001"));

        verifyNull(decoder, binary(
                "6767070088001050E2281400FFFFFFFF02334455660333445566043344556605AA00000007334455660A334455660B334455660C4E2000000DAA0000000E334455660F3344556610AAAA000011334455661C334455661F334455662133445566423344556646334455664D334455665C334455665E33445566880000000089000000008A000000008B00000000"));

        verifyPosition(decoder, binary(
                "676702001b03c5538086df0190c1790b3482df0f0157020800013beb00342401"));

    }

}
