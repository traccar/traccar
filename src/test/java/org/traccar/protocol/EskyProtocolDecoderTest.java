package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EskyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        EskyProtocolDecoder decoder = new EskyProtocolDecoder(null);

        verifyPosition(decoder, text(
                "EO;0;861311006461908;R;6;180420104751;2.97896;101.65091;0.75;320;3398;1;|"));

        verifyNull(decoder, text(
                "EL;1;864906029196626;170822143426;"));

        verifyPosition(decoder, text(
                "EO;0;864906029196626;R;7+170822143646+-26.10806+27.94600+0.40+0+0x1+0+102540+0+1242"));

        verifyPosition(decoder, text(
                "EO;0;864906029196626;R;0+170808155352+0.00000+0.00000+0.00+0+0x1+0+0+0+1233"));

        verifyPosition(decoder, text(
                "ET;1;014682000989425;R;0+171216001250+33.34405+-111.96682+0.00+0+0x1+0+25598+0+1257+0"));

    }

}
