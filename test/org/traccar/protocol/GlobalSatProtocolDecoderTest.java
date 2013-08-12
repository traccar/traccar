package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GlobalSatProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GlobalSatProtocolDecoder decoder = new GlobalSatProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "GSh,131826789036289,3,M,ea04*3d"));

        //TSPRXAB27GHKLMnaicz
        assertNotNull(decoder.decode(null, null,
                "GSr,1,135785412249986,01,I,EA02,3,230410,153318,E12129.2839,N2459.8570,0,1.17,212,8,1.0,12.3V*55"));
        
        assertNotNull(decoder.decode(null, null,
                "$355632004245866,1,1,040202,093633,E12129.2252,N2459.8891,00161,0.0100,147,07,2.4"));

        assertNotNull(decoder.decode(null, null,
                "$355632000959420,9,3,160413,230536,E03738.4906,N5546.3148,00000,0.3870,147,07,2.4"));

    }

}
