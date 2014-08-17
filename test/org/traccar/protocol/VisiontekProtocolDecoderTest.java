package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class VisiontekProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        VisiontekProtocolDecoder decoder = new VisiontekProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        verify(decoder.decode(null, null,
                "$1,AP09BU9397,861785006462448,20,06,14,15,03,28,17267339N,078279407E,060.0,073,0550,11,0,1,0,0,1,1,26,A,0000000000"));

        assertNull(decoder.decode(null, null,
                "$1,AP09BU9397,861785006462448,20,06,14,15,03,28,000000000,0000000000,000.0,000,0000,00,0,1,0,0,1,1,24,V,0000000000"));

    }

}
