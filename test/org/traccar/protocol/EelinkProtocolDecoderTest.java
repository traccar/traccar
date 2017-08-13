package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EelinkProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        EelinkProtocolDecoder decoder = new EelinkProtocolDecoder(new EelinkProtocol());

        // login (0x01)
        verifyNull(decoder, binary(
                "676701000c007b03525440717505180104"));

        // gps packet (0x02) with 10 bytes extra (device status/batt voltage/signal/adc1/adc2)
        verifyPosition(decoder, binary(
                "676702002509f65868507603a1e92e03cf90fe000000019f000117ee00111e0120631145003101510000"));
        
        // terminal state (0x05)
        verifyPosition(decoder, binary(
                "6767050022001F59643640000000000000000000000001CC0000249500142000015964A6C0006E"));

        // heartbeat (0x03)
        verifyAttributes(decoder, binary(
                "67670300040021006E"));

        // terminal state (0x05)
        verifyPosition(decoder, binary(
                "676705002200255964369D000000000000000000000001CC0000249500142000025964A71D006A"));

        // heartbeat (0x03)
        verifyAttributes(decoder, binary(
                "67670300040028006A"));

        // gps packet (0x02) with 10 bytes extra (device status/batt voltage/signal/adc1/adc2)
        verifyPosition(decoder, binary(
                "676702002509f65868507603a1e92e03cf90fe000000019f000117ee00111e0120631145003101510000"));

        // new proto - normal (0x12)
        verifyPosition(decoder, binary(
                "676712002d066c592cca6803002631a60b22127700240046005c08020d000301af000da0fd12007f11ce05820000001899c0"));
        
        // new proto - normal (0x12)
        verifyAttributes(decoder, binary(
                "676712001e0092579714d60201f90001785003a301cd1a006a118504f2000000000000"));        

        // new proto - normal (0x12)
        verifyPosition(decoder, binary(
                "676712003400505784cc0b130246479b07d05a06001800000000070195039f046100002cc52e6466b391604a4900890e7c00000000000006ca"));

        // new proto - warning (0x14)
        verifyPosition(decoder, binary(
                "676714002b00515784cc24130246479b07d05a06001800010000060195039f046100002cc52f6466b391604a49020089"));

        // new proto - report (0x15)
        verifyAttributes(decoder, binary(
                "676715000a00f75685510b0002006a"));
        
        // login (0x01)
        verifyNull(decoder, binary(
                "676701000c002603541880486128290120"));

        // alarm (0x04)
        verifyPosition(decoder, binary(
                "676704001c01a4569ff2dd0517a0f7020b0d9a06011000d8001e005b0004450183"));

        // terminal state (0x05)
        verifyPosition(decoder, binary(
                "676705002200ba569fc3520517a0d8020b0f740f007100d8001e005b0004460101569fd162001f"));

        // gps packet (0x02) with 10 bytes (device status/batt voltage/signal/adc1/adc2)
        verifyPosition(decoder, binary(
                "676702002500bb569fc3610517a091020b116000001900d8001e005b00044601001f1170003200000000"));
        

        // gps packet (0x02) only, no extra data
        verifyPosition(decoder, binary(
                "676702001b03c5538086df0190c1790b3482df0f0157020800013beb00342401"));        

        // alarm (0x04)
        verifyPosition(decoder, binary(
                "676704001c00b7569fc3020517a2d7020b08e100000000d8001e005b0004460004"));

        // login (0x01)
        verifyNull(decoder, binary(
                "676701000b001b035418804661834901"));

        // heartbeat (0x03)
        verifyAttributes(decoder, binary(
                "6767030004001A0001"));

        // obd data (0x07)
        verifyNull(decoder, binary(
                "6767070088001050E2281400FFFFFFFF02334455660333445566043344556605AA00000007334455660A334455660B334455660C4E2000000DAA0000000E334455660F3344556610AAAA000011334455661C334455661F334455662133445566423344556646334455664D334455665C334455665E33445566880000000089000000008A000000008B00000000"));
        
        // downlink data (0x80)
        verifyNotNull(decoder, binary(
                "676780005a00000100000000424154544552593a313030250a475052533a535543434553530a47534d3a4c4f570a4750533a434c4f5345442c300a4143433a4f46460a52454c41593a4f46460a504f5745523a4f4b0a4d533a4c4953334448"));
        
        // sms (0x06) PING? > PONG
        verifyPosition(decoder, binary(
                "67670600350338598754d70585d9b3fffe5b4c00000000ea000a56d80078b00032302b34343739383736353433323100000000000050494e473f"));

    }

}
