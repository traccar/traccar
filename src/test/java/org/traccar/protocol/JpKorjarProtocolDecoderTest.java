package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class JpKorjarProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new JpKorjarProtocolDecoder(null);

        verifyPosition(decoder, text(
                "KORJAR.PL,329587014519383,160910144240,52.247254N,021.013375E,0.00,1,F:4.18V,1 260 01 794B 3517,"));

        verifyPosition(decoder, text(
                "KORJAR.PL,329587014519383,160910144240,52.895515N,021.949151E,6.30,212,F:3.94V,0 260 01 794B 3519,"));

        verifyPosition(decoder, text(
                "KORJAR.PL,329587014519383,160910144240,52.895596N,021.949343E,12.46,087,L:2.18V,1 260 01 794B 3517,"));

    }

}
