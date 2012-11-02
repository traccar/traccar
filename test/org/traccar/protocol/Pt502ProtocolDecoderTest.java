package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Pt502ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {
        
        Pt502ProtocolDecoder decoder = new Pt502ProtocolDecoder(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "$POS,6094,205523.000,A,1013.6223,N,06728.4248,W,0.0,99.3,011112,,,A/00000,00000/0/23895000//"));

    }

}
