package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.traccar.helper.DecoderVerifier.verify;

public class VisiontekProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        VisiontekProtocolDecoder decoder = new VisiontekProtocolDecoder(new VisiontekProtocol());
        
        //verify(decoder.decode(null, null,
        //        "$1,AP116,05,06,15,11,48,32,1725.0460N,07824.5289E,0617,07,0,030,2091,0,0,0,1,1,1,1,20,00.0000,00.3820,00.0000,VAJRA V1.00,A#"));

        verify(decoder.decode(null, null,
                "$1,AP09BU9397,861785006462448,20,06,14,15,03,28,17267339N,078279407E,060.0,073,0550,11,0,1,0,0,1,1,26,A,0000000000"));

        assertNull(decoder.decode(null, null,
                "$1,AP09BU9397,861785006462448,20,06,14,15,03,28,000000000,0000000000,000.0,000,0000,00,0,1,0,0,1,1,24,V,0000000000"));
        
        assertNull(decoder.decode(null, null,
                "$1,1234567890,02,06,11,17,07,45,00000000,000000000,00.0,0,0,V"));

        verify(decoder.decode(null, null,
                "$1,1234567890,02,06,11,17,07,45,17267690N,078279340E,060.0,113,0,A"));

    }

}
