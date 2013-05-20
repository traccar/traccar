package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Pt502ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Pt502ProtocolDecoder decoder = new Pt502ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "$POS,6094,205523.000,A,1013.6223,N,06728.4248,W,0.0,99.3,011112,,,A/00000,00000/0/23895000//"));

        assertNotNull(decoder.decode(null, null,
                "$POS,6120,233326.000,V,0935.1201,N,06914.6933,W,0.00,,151112,,,A/00000,00000/0/0/"));

        assertNotNull(decoder.decode(null, null,
                "$POS,6002,233257.000,A,0931.0430,N,06912.8707,W,0.05,146.98,141112,,,A/00010,00000/0/5360872"));

        assertNotNull(decoder.decode(null, null,
                "$POS,6095,233344.000,V,0933.0451,N,06912.3360,W,,,151112,,,N/00000,00000/0/1677600/"));

        /*assertNotNull(decoder.decode(null, null,
                "$PHO0,6091,233606.000,A,0902.9855,N,06944.3654,W,0.0,43.8,141112,,,A/00010,00000/0/224000//"));*/
        
        assertNotNull(decoder.decode(null, null,
                "$POS,353451000164,082405.000,A,1254.8501,N,10051.6752,E,0.00,237.99,160513,,,A/0000,0/0/55000//a71/"));


    }

}
