package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class YwtProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        YwtProtocolDecoder decoder = new YwtProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "%GP,3000012345:0,090723182813,E114.602345,N22.069725,,30,160,4,0,00,,2794-10FF-46000,3>0-0"));

        assertNotNull(decoder.decode(null, null,
                "%RP,3000012345:0,090807182815,E114.602345,N22.069725,,30,160,4,0,00"));

        assertNotNull(decoder.decode(null, null,
                "%KP,3000012345:0,090807183115,E114.602345,N22.069725,,30,160,5,0,00;"));

    }

}
