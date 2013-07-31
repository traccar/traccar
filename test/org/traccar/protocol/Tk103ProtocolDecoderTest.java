package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Tk103ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tk103ProtocolDecoder decoder = new Tk103ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNull(decoder.decode(null, null, "(090411121854BP0000001234567890HSO"));

        assertNotNull(decoder.decode(null, null,
                "(035988863964BP05000035988863964110524A4241.7977N02318.7561E000.0123536356.5100000000L000946BB"));

        assertNotNull(decoder.decode(null, null,
                "(013632782450BP05000013632782450120803V0000.0000N00000.0000E000.0174654000.0000000000L00000000"));

        assertNotNull(decoder.decode(null, null,
                "(013666666666BP05000013666666666110925A1234.5678N01234.5678W000.002033490.00000000000L000024DE"));
        
        assertNotNull(decoder.decode(null, null,
                "(013666666666BO012110925A1234.5678N01234.5678W000.0025948118.7200000000L000024DE"));

        assertNotNull(decoder.decode(null, null,
                "\n\n\n(088045133878BR00130228A5124.5526N00117.7152W000.0233614352.2200000000L01B0CF1C"));
        
        assertNotNull(decoder.decode(null, null,
                "(008600410203BP05000008600410203130721A4152.5790N01239.2770E000.0145238173.870100000AL0000000"));
        
        assertNotNull(decoder.decode(null, null,
                "(013012345678BR00130515A4843.9703N01907.6211E000.019232800000000000000L00009239"));

    }

}
