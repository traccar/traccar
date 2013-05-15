package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class V680ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        V680ProtocolDecoder decoder = new V680ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNull(decoder.decode(null, null,
                "#353588102019155"));
        
        assertNotNull(decoder.decode(null, null,
                "#356823031193431##0#0000#SF#1#72403#V#04702.3025,W,2252.18380,S,008.18,0#090413#134938"));

        assertNotNull(decoder.decode(null, null,
                "#356823033219838#1000#0#1478#AUT#1#66830FFB#03855.6628,E,4716.6821,N,001.41,259#130812#143905"));

        assertNotNull(decoder.decode(null, null,
                "356823033219838#1000#0#1478#AUT#1#66830FFB#03855.6628,E,4716.6821,N,001.41,259#130812#143905"));

        assertNotNull(decoder.decode(null, null,
                "#353588102019155##1#0000#AUT#01#7240060be7873f#4849.079800,W,2614.458200,S,0.00,0.00#130413#182110.000"));
        
        assertNotNull(decoder.decode(null, null,
                "#353588302045917##1#0000#AUT#01#7243141c2b14c3#4738.442300,W,2334.874000,S,0.00,0.30#170413#004831.000"));
        
        assertNotNull(decoder.decode(null, null,
                "#352897045085282##0#0000#AUT#1#72400510730208,00d36307,10734fc4#4647.8922,W,2339.1956,S,2.60,63.74#200413#094310.000"));

    }

}
