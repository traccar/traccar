package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class LaipacProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        LaipacProtocolDecoder decoder = new LaipacProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "$AVSYS,99999999,V1.50,SN0000103,32768*15"));
        
        assertNull(decoder.decode(null, null, "$ECHK,99999999,0*35"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*37"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,a,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*17"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,v,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*00"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,r,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*04"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,S,3.727,17,1,0,0*54"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,T,3.727,17,1,0,0*53"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,3,3.727,17,1,0,0*34"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,X,3.727,17,1,0,0*5F"));

        assertNotNull(decoder.decode(null, null,
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,4,3.727,17,1,0,0*33"));

    }

}
