package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class CradlepointProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new CradlepointProtocolDecoder(null));

        verifyPosition(decoder, text(
                "356526070063940,0,4337.19009,N,11612.34705,W,0.0,277.2,AT&T,,,-79,,-14.0,"));

        verifyPosition(decoder, text(
                "356526070063940,1,4337.19008,N,11612.34705,W,0.0,277.2,AT&T,,,-79,,-14.0,"));

        verifyPosition(decoder, text(
                "+14063964266,162658,4333.62404,N,11636.23469,W,0.0,,Verizon Wireless,LTE,-107,-74,-16,,100.68.169.178"));

        verifyPosition(decoder, text(
                "+12084014675,162658,4337.174385,N,11612.338373,W,0.0,,Verizon,,-71,-44,-11,,"));

        verifyPosition(decoder, text(
                "353547063544681,170515,3613.25,N,11559.14,W,0.0,,,,,,,,"));

        verifyPosition(decoder, text(
                "353547060558130,170519,4337.17,N,11612.34,W,0.0,294.7,,,,,,,"));

        verifyPosition(decoder, text(
                "+12084014675,162658,4337.174385,N,11612.338373,W,0.0,,Verizon,,-71,-44,-11,,"));

    }

}
