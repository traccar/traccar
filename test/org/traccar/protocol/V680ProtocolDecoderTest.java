package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class V680ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        V680ProtocolDecoder decoder = new V680ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNotNull(decoder.decode(null, null,
                "#356823031193431##0#0000#SF#1#72403#V#04702.3025,W,2252.18380,S,008.18,0#090413#134938"));

        assertNotNull(decoder.decode(null, null,
                "#356823033219838#1000#0#1478#AUT#1#66830FFB#03855.6628,E,4716.6821,N,001.41,259#130812#143905"));

        assertNotNull(decoder.decode(null, null,
                "356823033219838#1000#0#1478#AUT#1#66830FFB#03855.6628,E,4716.6821,N,001.41,259#130812#143905"));

    }

}
