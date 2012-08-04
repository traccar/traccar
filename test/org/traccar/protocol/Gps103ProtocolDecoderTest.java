package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Gps103ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {
        
        Gps103ProtocolDecoder decoder = new Gps103ProtocolDecoder(new TestDataManager(), 0);
        
        // assertNull(decoder.decode(null, null, "##,imei:10000000000000,A"));

        assertNotNull(decoder.decode(null, null,
                "imei:10000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451042861763,tracker,1106132241,,F,144114.000,A,2301.9052,S,04909.3676,W,0.13,"));

        assertNotNull(decoder.decode(null, null,
                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,"));
        
        assertNotNull(decoder.decode(null, null,
                "imei:353451049926460,tracker,1208042043,123456 99008026,F,124336.000,A,3509.8668,N,03322.7636,E,0.00,,"));

    }

}
