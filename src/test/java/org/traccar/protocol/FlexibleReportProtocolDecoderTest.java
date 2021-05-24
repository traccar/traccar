package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FlexibleReportProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new FlexibleReportProtocolDecoder(null);

        verifyAttributes(decoder, binary(
                "7D010860112040978399000027E3CFC30130002E7FFFFFFF0C00000000055D4A800ABA9500000000000000002F5D0E800000000000FFFFFFFF158A0000000000FFFF"));

    }

}
