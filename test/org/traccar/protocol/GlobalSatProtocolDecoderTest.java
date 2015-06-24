package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.traccar.helper.DecoderVerifier.verify;

public class GlobalSatProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GlobalSatProtocolDecoder decoder = new GlobalSatProtocolDecoder(new GlobalSatProtocol());

        assertNull(decoder.decode(null, null, "GSh,131826789036289,3,M,ea04*3d"));

        decoder.setFormat0("SORPZAB27GHKLMN*U!");

        verify(decoder.decode(null, null,
                "GSr,011412001878820,4,5,00,,1,250114,105316,E00610.2925,N4612.1824,0,0.02,0,1,0.0,64*51!"));

        verify(decoder.decode(null, null,
                "GSr,357938020310710,,4,04,,1,170315,060657,E00000.0000,N0000.0000,148,0.00,0,0,0.0,11991mV*6c!"));

        decoder.setFormat0("TSPRXAB27GHKLMnaicz*U!");

        verify(decoder.decode(null, null,
                "GSr,1,135785412249986,01,I,EA02,3,230410,153318,E12129.2839,N2459.8570,0,1.17,212,8,1.0,12.3V*55"));

        verify(decoder.decode(null, null,
                "GSr,GTR-128,012896009148443,0040,5,0080,3,190813,185812,W11203.3661,N3330.2104,344,0.24,78,9,0.8,60%,0,0,12,\"310,410,0bdd,050d,02,21\",\"310,410,0bdd,0639,24,7\"*79"));

        verify(decoder.decode(null, null,
                "$355632004245866,1,1,040202,093633,E12129.2252,N2459.8891,00161,0.0100,147,07,2.4"));

        verify(decoder.decode(null, null,
                "$355632000959420,9,3,160413,230536,E03738.4906,N5546.3148,00000,0.3870,147,07,2.4"));
        
        verify(decoder.decode(null, null,
                "$353681041893264,9,3,240913,100833,E08513.0122,N5232.9395,181.3,22.02,251.30,9,1.00"));

        /*verify(decoder.decode(null, null,
                "$353681041893264,9,4,230913,052449,\"250,99,B443,422E,42,37\",\"250,99,B443,4232,43,44\",\"250,99,B443,7910,40,32\",\"250,99,B443,B456,40,28\",\"250,99,B443,B455,40,27\""));*/

        decoder.setFormat0("SPRXYAB27GHKLMmnaefghiotuvwb*U!");
        
        verify(decoder.decode(null, null,
                "GSr,GTR-128,013227006963064,0080,1,a080,3,190615,163816,W07407.7134,N0440.8601,2579,0.01,130,12,0.7,11540mV,0,77,14,\"732,123,0744,2fc1,41,23\",\"732,123,0744,2dfe,05,28\",\"732,123,0744,272a,15,21\",\"732,123,0744,2f02,27,23\"*3b!"));

    }

}
