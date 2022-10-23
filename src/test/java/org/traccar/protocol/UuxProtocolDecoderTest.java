package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class UuxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new UuxProtocolDecoder(null));

        verifyNull(decoder, binary(
                "81910b01ff"));

        verifyAttributes(decoder, binary(
                "81910c5a9031395533443630363631051e061a1e07397079712a000000000000413133333135332e333939304e30333531322e393837324530303031303030303000000200000000001f303036323236303030303030303030300000ffff"));

        verifyAttributes(decoder, binary(
                "81918c2d9e31395533443630363631041c0c16043030313030300007000000000000000000000000000000000000000000"));

    }

}
