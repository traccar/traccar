package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TrackboxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TrackboxProtocolDecoder(null));

        verify(decoder, text(
                "a=connect&v=11&i=111111111111111"));

        verify(decoder, text(
                "183457.999,5126.0247N,00002.8686E,5.2,70.4,3,57.63,32.11,17.32,150507,05"),
                position().location("2007-05-15T18:34:57.999Z", true, 51.43375, 0.04781));

        verify(decoder, text(
                "183558.999,5126.3979N,00003.0745E,5.2,70.4,3,57.63,32.11,17.32,150507,05"),
                position());

    }

}
