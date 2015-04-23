package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TelikProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TelikProtocolDecoder decoder = new TelikProtocolDecoder(null);

        assertNull(decoder.decode(null, null,
                "0026436729|232|01|003002030"));

        verify(decoder.decode(null, null,
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7"));
        
    }

}
