package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GpsGateProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GpsGateProtocolDecoder decoder = new GpsGateProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "$FRLIN,,user1,8IVHF*7A"));

        assertNull(decoder.decode(null, null, "$FRLIN,IMEI,1234123412341234,*7B"));

        assertNotNull(decoder.decode(null, null,
                "$GPRMC,154403.000,A,6311.64120,N,01438.02740,E,0.000,0.0,270707,,*0A"));

    }

}
