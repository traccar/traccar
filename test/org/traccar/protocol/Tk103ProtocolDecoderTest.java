package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Tk103ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {
        
        Tk103ProtocolDecoder decoder = new Tk103ProtocolDecoder(new TestDataManager(), 0);
        
        assertNotNull(decoder.decode(null, null,
                "(035988863964BP05000035988863964110524A4241.7977N02318.7561E000.0123536356.5100000000L000946BB"));

    }

}
