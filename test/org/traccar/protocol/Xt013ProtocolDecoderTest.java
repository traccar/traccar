package org.traccar.protocol;

import org.junit.Test;
import org.traccar.helper.TestDataManager;

import static org.traccar.helper.DecoderVerifier.verify;

public class Xt013ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Xt013ProtocolDecoder decoder = new Xt013ProtocolDecoder(new TestDataManager(), null, null);
        
        verify(decoder.decode(null, null,
                "TK,862950021650364,150118113832,+53.267722,+5.767143,0,86,12,0,F,204,08,C94,336C,22,,4.21,1,,,,,,,,"));

    }

}
