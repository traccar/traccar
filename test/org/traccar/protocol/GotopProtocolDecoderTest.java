package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GotopProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GotopProtocolDecoder decoder = new GotopProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, ""));
        
        assertNull(decoder.decode(null, null, "353327020412763,CMD-X"));

        verify(decoder.decode(null, null,
                "013226009991924,CMD-T,A,DATE:130802,TIME:153721,LAT:25.9757433S,LOT:028.1087816E,Speed:000.0,X-X-X-X-81-26,000,65501-00A0-4B8E"));

        verify(decoder.decode(null, null,
                "353327020115804,CMD-T,A,DATE:090329,TIME:223252,LAT:22.7634066N,LOT:114.3964783E,Speed:000.0,84-20,000"));
        
        verify(decoder.decode(null, null,
                "353327020115804,CMD-T,A,DATE:090329,TIME:223252,LAT:22.7634066N,LOT:114.3964783E,Speed:000.0,1-1-0-84-20,000"));
        
        verify(decoder.decode(null, null,
                "353327020412763,CMD-F,V,DATE:140125,TIME:183636,LAT:51.6384466N,LOT:000.2863866E,Speed:000.0,61-19,"));
        
    }

}
