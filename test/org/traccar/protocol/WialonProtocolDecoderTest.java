package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WialonProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WialonProtocolDecoder decoder = new WialonProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "#L#123456789012345;test"));
        
        assertNull(decoder.decode(null, null, "#L#2002;NA"));
        
        assertNull(decoder.decode(null, null, "#P#"));

        verify(decoder.decode(null, null,
                "#SD#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4"));

        verify(decoder.decode(null, null,
                "#D#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4;0.0;0;0;14.77,0.02,3.6;NA;count1:1:564,fuel:2:45.8,hw:3:V4.5"));
        
        verify(decoder.decode(null, null,
                "#D#190114;051312;4459.6956;N;04105.9930;E;35;306;204.000000;12;NA;452986639;NA;106.000000;NA;sats_gps:1:9,sats_glonass:1:3,balance:2:12123.000000,stay_balance:1:0"));

    }

}
