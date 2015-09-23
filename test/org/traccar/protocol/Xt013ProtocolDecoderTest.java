package org.traccar.protocol;

import org.junit.Test;

import static org.traccar.helper.DecoderVerifier.verify;

public class Xt013ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Xt013ProtocolDecoder decoder = new Xt013ProtocolDecoder(new Xt013Protocol());
        
        verify(decoder.decode(null, null,
                "TK,862950021650364,150131090859,+53.267863,+5.767363,0,38,12,0,F,204,08,C94,336C,24,,4.09,1,,,,,,,,"));
        
        verify(decoder.decode(null, null,
                "TK,862950021650364,150118113832,+53.267722,+5.767143,0,86,12,0,F,204,08,C94,336C,22,,4.21,1,,,,,,,,"));
        
        verify(decoder.decode(null, null,
                "HI,862950021650364TK,862950021650364,150118113832,+53.267722,+5.767143,0,86,12,0,F,204,08,C94,336C,22,,4.21,1,,,,,,,,"));

    }

}
