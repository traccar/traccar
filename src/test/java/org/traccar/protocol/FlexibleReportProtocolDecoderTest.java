package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FlexibleReportProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FlexibleReportProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "7d010015875000013001001028fd98991830002e7fffffff0c28fd989903f6540a07f250ed00000f02f2140f5ea20000000000000202d4000a1f8b0100000708ffff"));

        verifyAttributes(decoder, binary(
                "7D010860112040978399000027E3CFC30130002E7FFFFFFF0C00000000055D4A800ABA9500000000000000002F5D0E800000000000FFFFFFFF158A0000000000FFFF"));

    }

}
