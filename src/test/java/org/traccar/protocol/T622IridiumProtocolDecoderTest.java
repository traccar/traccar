package org.traccar.protocol;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class T622IridiumProtocolDecoderTest extends ProtocolTest {

    @Disabled
    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new T622IridiumProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "01003301001c2a8cef8333303034333430363735343836353000001700006461d512020011232f03a0fff1c85d0612b3f02b00000048"));

    }

}
