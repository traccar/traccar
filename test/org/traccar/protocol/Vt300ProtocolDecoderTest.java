package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Vt300ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Vt300ProtocolDecoder decoder = new Vt300ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "210000001,20070313170040,121.123456,12.654321,0,233,0,9,2,0.0,0,0.00,0.00,0"));

    }

}
