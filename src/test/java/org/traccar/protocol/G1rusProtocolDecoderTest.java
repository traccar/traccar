package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class G1rusProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new G1rusProtocolDecoder(null));

        verifyPositions(decoder, binary(
                "f84604000d85f5e55298004b012ad7d524003d1d0f50726f6d61205361742031303030201556312e31302656312e312e340a03120fa0142b8475a157050401846dfc060175120a29a10d02440111002100310041005107d10ef8"));

    }

}
