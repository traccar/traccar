package org.traccar.protocol;


import org.junit.Test;
import org.traccar.ProtocolTest;

public class JpKorjarProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        JpKorjarProtocolDecoder decoder = new JpKorjarProtocolDecoder(new JpKorjarProtocol());

        verifyPosition(decoder, text(
                "KORJAR.PL,329587014519383,160910144240,52.247254N,021.013375E,0.00,1,F:4.18V,1 260 01 794B 3517,"));

    }

}
