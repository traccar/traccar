package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xt013ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xt013ProtocolDecoder(null));
        
        verifyPosition(decoder, text(
                "TK,862950021650364,150131090859,+53.267863,+5.767363,0,38,12,0,F,204,08,C94,336C,24,,4.09,1,,,,,,,,"),
                position("2015-01-31 09:08:59.000", true, 53.26786, 5.76736));

        verifyPosition(decoder, text(
                "TK,862950021650364,150118113832,+53.267722,+5.767143,0,86,12,0,F,204,08,C94,336C,22,,4.21,1,,,,,,,,"));
        
        verifyPosition(decoder, text(
                "HI,862950021650364TK,862950021650364,150118113832,+53.267722,+5.767143,0,86,12,0,F,204,08,C94,336C,22,,4.21,1,,,,,,,,"));

    }

}
