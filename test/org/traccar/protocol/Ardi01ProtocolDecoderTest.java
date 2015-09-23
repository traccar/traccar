package org.traccar.protocol;

import org.junit.Test;

import static org.traccar.helper.DecoderVerifier.verify;

public class Ardi01ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Ardi01ProtocolDecoder decoder = new Ardi01ProtocolDecoder(new Ardi01Protocol());

        verify(decoder.decode(null, null,
                "013227003054776,20141010052719,24.4736042,56.8445807,110,289,40,7,5,78,-1"));

    }

}
