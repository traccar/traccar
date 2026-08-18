package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class AkrozProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AkrozProtocolDecoder(null));

        // RUV01 - real device capture, valid fix
        verifyPosition(decoder, buffer(
                ">RUV0100,NT003,270726121842-2435198-05082456074149300DE0001 24090921,04222828,00000,00000,00000,278982,267781905,01221,00082,00022,00099,0,1,4G:0,00000,860813076678627;ID=PAFA;#8048;*0C<"),
                position("2026-07-27 12:18:42.000", true, -24.35198, -50.82456));

        // RUV01 - documentation sample, no fix
        verifyAttribute(decoder, buffer(
                ">RUV01100,NT003,240622184546-1926542-046895630000009FFDE0000,04221376,00000,00000,00000,1111111111,2222222222,03333,00444,55555,00100,0,0,4G:1,1644991399;ID=0081;#8012;*18<"),
                Position.KEY_DRIVER_UNIQUE_ID, "1644991399");

        // RUV02 - end of trip
        verifyAttribute(decoder, buffer(
                ">RUV0200,NT003,030822190731-3597296-062735570000009035F0000,11111,22302,33333,44444,55555,66666,77777,88888,00361,600000,300000;ID=0081;#8020;*04<"),
                Position.KEY_FUEL_USED, 30000.0);

        // RUV03 - engine (CAN) report
        verifyPosition(decoder, buffer(
                ">RUV03150,NT003,030822190731-3597296-062735570000009035F0000,65,69584,120455002,2312,86,10,29,44895330,0,40,36,0,0,0,0,0,0,0,0,0;ID=0083;#0030;*2F<"));

        verifyAttribute(decoder, buffer(
                ">RUV03150,NT003,030822190731-3597296-062735570000009035F0000,65,69584,120455002,2312,86,10,29,44895330,0,40,36,0,0,0,0,0,0,0,0,0;ID=0083;#0030;*2F<"),
                Position.KEY_OBD_SPEED, 40);

        verifyAttribute(decoder, buffer(
                ">RUV03150,NT003,030822190731-3597296-062735570000009035F0000,65,69584,120455002,2312,86,10,29,44895330,0,40,36,0,0,0,0,0,0,0,0,0;ID=0083;#0030;*2F<"),
                Position.KEY_FUEL_USED, 4489533.0);

        // RUV03 - periodic tracking, no CAN speed
        verifyAttribute(decoder, buffer(
                ">RUV03153,NT003,270726133035-2353539-046680190000009FFDE0000,0,0,0,500,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0;ID=0000;#80E5;*22<"),
                Position.KEY_RPM, 500);

        // Event codes (ignition / periodic tracking)
        verifyAttribute(decoder, buffer(
                ">RUV01100,NT003,240622184546-1926542-046895630000009FFDE0000,04221376,00000,00000,00000,1111111111,2222222222,03333,00444,55555,00100,0,0,4G:1,1644991399;ID=0081;#8012;*18<"),
                Position.KEY_EVENT, 100);

        verifyAttribute(decoder, buffer(
                ">RUV01102,NT003,240622184546-1926542-046895630000009FFDE0000,04221376,00000,00000,00000,1111111111,2222222222,03333,00444,55555,00100,0,0,4G:1,1644991399;ID=0081;#8012;*1A<"),
                Position.KEY_EVENT, 102);

        verifyAttribute(decoder, buffer(
                ">RUV03152,NT003,030822190731-3597296-062735570000009035F0000,65,69584,120455002,2312,86,10,29,44895330,0,40,36,0,0,0,0,0,0,0,0,0;ID=0083;#0030;*2D<"),
                Position.KEY_EVENT, 152);

    }

}
