package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WialonProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WialonProtocolDecoder decoder = new WialonProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "#L#123456789012345;test"));
        
        assertNull(decoder.decode(null, null, "#P#"));

        assertNotNull(decoder.decode(null, null,
                "#SD#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4"));

        assertNotNull(decoder.decode(null, null,
                "#D#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4;0.0;0;0;14.77,0.02,3.6;NA;count1:1:564,fuel:2:45.8,hw:3:V4.5"));

    }

}
