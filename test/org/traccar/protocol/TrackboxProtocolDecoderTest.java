package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TrackboxProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TrackboxProtocolDecoder decoder = new TrackboxProtocolDecoder(new TrackboxProtocol());

        assertNull(decoder.decode(null, null, "a=connect&v=11&i=111111111111111"));

        verify(decoder.decode(null, null,
                "183457.999,5126.0247N,00002.8686E,5.2,70.4,3,57.63,32.11,17.32,150507,05"));

        verify(decoder.decode(null, null,
                "183558.999,5126.3979N,00003.0745E,5.2,70.4,3,57.63,32.11,17.32,150507,05"));

    }

}
