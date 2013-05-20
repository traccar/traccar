package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class H02ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        H02ProtocolDecoder decoder = new H02ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "*HQ,3800008786,V1,062507,V,3048.2437,N,03058.5617,E,000.00,000,250413,FFFFFBFF"));

        assertNotNull(decoder.decode(null, null,
                "*HQ,123456789012345,V1,155850,A,5214.5346,N,2117.4683,E,0.00,270.90,131012,ffffffff,000000,000000,000000,000000"));
        
        assertNotNull(decoder.decode(null, null,
                "*HQ,353588010001689,V1,221116,A,1548.8220,S,4753.1679,W,0.00,0.00,300413,ffffffff,0002d4,000004,0001cd,000047"));

        assertNotNull(decoder.decode(null, null,
                "*HQ,354188045498669,V1,195200,A,701.8915,S,3450.3399,W,0.00,205.70,050213,ffffffff,000243,000000,000000"));
        

    }

}
