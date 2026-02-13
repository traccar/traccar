package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class T622IridiumProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new T622IridiumProtocolDecoder(null));

        decoder.setFormat("01,02,03,04,05,08");

        verifyPosition(decoder, binary(
                "01003501001c68b2cb1733303034333430363735343836353000016e000064b5f497020013234c5ea0ff1c365d0600b1482c010000cf0004"),
                position("2023-07-18 02:10:08.000", true, -6.26732, 106.77200));

    }

}
