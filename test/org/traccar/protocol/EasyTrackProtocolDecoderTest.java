package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class EasyTrackProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        EasyTrackProtocolDecoder decoder = new EasyTrackProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "*ET,135790246811221,GZ,0001,0005"));

        assertNotNull(decoder.decode(null, null,
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123"));

        assertNotNull(decoder.decode(null, null,
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123,100"));

        assertNotNull(decoder.decode(null, null,
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,8413FA4E,0000,0000,00000000,20,4,0000,00F123,100"));

    }

}
