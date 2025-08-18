package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class EskyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new EskyProtocolDecoder(null));

        verifyPosition(decoder, text(
                "ET;1;864431045310325;R;0+240303180628+-33.40958+149.56797+0.00+63+0xb+0+17814846+1168+0+0+WVWZZZ6RZFY242201+135+DTCNULL"));

        verifyAttribute(decoder, text(
                "ET;0;860337031066546;R;9+200717114059+41.32053+19.80761+0.30+0+0x2+8+40381744+0+1409+11"),
                Position.KEY_BATTERY, 14.09);

        verifyAttribute(decoder, text(
                "ET;0;860337031078319;R;6+190317162511+41.32536+19.83144+0.14+0+0x0+0+18460312+0+1233+192"),
                Position.KEY_IGNITION, true);

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
