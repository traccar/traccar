package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GpsGateProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GpsGateProtocolDecoder decoder = new GpsGateProtocolDecoder(null);

        assertNull(decoder.decode(null, null, "$FRLIN,,user1,8IVHF*7A"));
        
        assertNull(decoder.decode(null, null, "$FRLIN,,354503026292842,VGZTHKT*0C"));

        assertNull(decoder.decode(null, null, "$FRLIN,IMEI,1234123412341234,*7B"));
        
        assertNull(decoder.decode(null, null, "$FRLIN,,saab93_device,KLRFBGIVDJ*28"));

        verify(decoder.decode(null, null,
                "$GPRMC,154403.000,A,6311.64120,N,01438.02740,E,0.000,0.0,270707,,*0A"));
        
        verify(decoder.decode(null, null,
                "$GPRMC,074524,A,5553.73701,N,03728.90491,E,10.39,226.5,160614,0.0,E*75"));

    }

}
