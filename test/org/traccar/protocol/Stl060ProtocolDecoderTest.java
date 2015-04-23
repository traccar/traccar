package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Stl060ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Stl060ProtocolDecoder decoder = new Stl060ProtocolDecoder(null);

        verify(decoder.decode(null, null,
                "$1,357804048043099,D001,AP29AW0963,23/02/14,14:06:54,17248488N,078342226E,0.08,193.12,1,1,1,1,1,A"));
        
        verify(decoder.decode(null, null,
                "$1,357804048043099,D001,AP29AW0963,12/05/14,07:39:57,1724.8564N,07834.2199E,0.00,302.84,1,1,1,1,1,A"));
        
        verify(decoder.decode(null, null,
                "$1,357804047969310,D001,AP29AW0963,01/01/13,13:24:47,1723.9582N,07834.0945E,00100,010,0,0,0,0,0,A,"));

    }

}
