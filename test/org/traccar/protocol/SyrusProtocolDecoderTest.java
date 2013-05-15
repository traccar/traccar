package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class SyrusProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        SyrusProtocolDecoder decoder = new SyrusProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                ">RPV15714+3739438-1220384601512612;ID=1234;*7F"));

        assertNotNull(decoder.decode(null, null,
                "\r\n>REV691615354941+3570173+1397742703203212;ID=Test"));

        assertNotNull(decoder.decode(null, null,
                ">REV481599462982+2578391-0802945201228512;ID=Test"));

    }

}
