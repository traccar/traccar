package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FlexCommProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FlexCommProtocolDecoder(null));

        verifyPosition(decoder, text(
                "7E00865067022408382201705302358271024932258006712785200700022601010224100040002C5002A2210001000000010012342107"));

        verifyPosition(decoder, text(
                "7E27865067022408382201705241211301024932197006712794000910022481008234100040002C5002A2200011000000006306941827"));

    }

}
