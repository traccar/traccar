package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Gps056FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Gps056FrameDecoder());

        assertEquals(
                binary("242435314750534c5f30323836323436323033333738323934361905110f160b0b7710584e1cbd1b9b4500005b100300fb0a071700ffff23"),
                decoder.decode(null, null, binary("242435314750534c5f30323836323436323033333738323934361905110f160b0b7710584e1cbd1b9b4500005b100300fb0a071700ffff230030")));

        assertEquals(
                binary("242432354c4f474e5f3131383632343632303333373832393436322e3123"),
                decoder.decode(null, null, binary("242432354c4f474e5f3131383632343632303333373832393436322e3123")));

    }

}
