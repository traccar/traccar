package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class BoxProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        BoxProtocolDecoder decoder = new BoxProtocolDecoder(new BoxProtocol());
        
        assertNull(decoder.decode(null, null,
                "H,BT,358281002435893,081028142432,F5813D19,6D6E6DC2"));
        
        assertNull(decoder.decode(null, null,
                "H,BT,N878123,080415081234,D63E6DD9,6D6E6DC2,8944100300825505377"));

        verify(decoder.decode(null, null,
                "L,081028142429,G,52.51084,-1.70849,0,170,0,1,0"));

        verify(decoder.decode(null, null,
                "L,081028142432,G,52.51081,-1.70849,0,203,0,16,0"));

        assertNull(decoder.decode(null, null,
                "L,080528112501,AI1,145.56"));

        assertNull(decoder.decode(null, null,
                "E,1"));

    }

}
