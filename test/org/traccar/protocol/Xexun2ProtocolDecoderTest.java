package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Xexun2ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Xexun2ProtocolDecoder decoder = new Xexun2ProtocolDecoder(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120009.590,A,4639.6774,N,01418.5737,E,0.00,0.00,111111,,,A*68,F,, imei:359853000144328,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576"));

        assertNotNull(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,help me!, imei:123456789012345,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576"));

    }

}
