package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Ev603ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {
        
        Ev603ProtocolDecoder decoder = new Ev603ProtocolDecoder(new TestDataManager(), 0);
        
        assertNull(decoder.decode(null, null, "!5,17,V"));

        assertNotNull(decoder.decode(null, null,
                "!A,26/10/12,00:28:41,7.770385,-72.215706,0.0,25101,0"));

        assertNotNull(decoder.decode(null, null,
                "!A,01/12/10,13:25:35,22.641724,114.023666,000.1,281.6,0"));

    }

}
