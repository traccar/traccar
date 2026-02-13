package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PortmanProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PortmanProtocolDecoder(null));

        verifyPosition(decoder, text(
                "%%863922034547720,A,231119031316,N3640.4542E11707.5992,000,000,NA,95000000,NA,254,24,1.00,24"));

        verifyPosition(decoder, text(
                "$EXT,P0RTMANGRANT,A,210609201710,N0951.6879W08357.0129,0,0,NA,NA,11,25,174700.25,NA,01820000,108"));

        verifyPosition(decoder, text(
                "$PTMLA,355854050074633,A,200612153351,N2543.0681W10009.2974,0,190,NA,C9830000,NA,108,8,2.66,16,GNA"));

    }

}
