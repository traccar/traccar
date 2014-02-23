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
                "$1,357804047969310,D001,AP29AW0963,01/01/13,13:24:47,1723.9582N,07834.0945E,00100,010,0,0,0,0,0,A,"));

    }

}
