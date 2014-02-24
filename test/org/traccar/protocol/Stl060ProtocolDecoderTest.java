package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Stl060ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Stl060ProtocolDecoder decoder = new Stl060ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        verify(decoder.decode(null, null,
                "$1,357804048043099,D001,AP29AW0963,23/02/14,14:06:54,17248488N,078342226E,0.08,193.12,1,1,1,1,1,A"));

    }

}
