package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class SyrusProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        SyrusProtocolDecoder decoder = new SyrusProtocolDecoder(null, false);

        assertNotNull(decoder.decode(null, null,
                ">RPV00000+3739438-1220384601512612;ID=1234;*7F"));

        verify(decoder.decode(null, null,
                "\r\n>REV691615354941+3570173+1397742703203212;ID=Test"));

        verify(decoder.decode(null, null,
                ">REV481599462982+2578391-0802945201228512;ID=Test"));
        
        verify(decoder.decode(null, null,
                ">REV131756153215+3359479-0075299001031332;VO=10568798;IO=310;SV=10;BL=4190;CV09=0;AD=0;AL=+47;ID=356612021059680"));
        
        assertNotNull(decoder.decode(null, null,
                ">RPV02138+4555512-0735478000000032;ID=1005;*76<"));
        
        assertNotNull(decoder.decode(null, null,
                ">RPV19105+4538405-0739518900000012;ID=9999;*7A<\r\n"));


    }

}
