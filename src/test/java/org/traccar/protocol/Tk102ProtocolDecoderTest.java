package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Tk102ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Tk102ProtocolDecoder(null));

        verifyNull(decoder, buffer(
                "[\u00800000000000\u000821315452]"));

        verifyNull(decoder, buffer(
                "[\u00f00000000000\u000821315452]"));

        verifyPosition(decoder, buffer(
                "[\u00900100100001\u0036(ONE025857A2232.0729N11356.0030E000.02109110100000000)]"));

        verifyPosition(decoder, buffer(
                "[\u00900100100001\u0036(ITV025857A2232.0729N11356.0030E000.02109110100000000)]"));

        verifyNull(decoder, buffer(
                "[\u00210000000081\u0072(353327023367238,TK102-W998_01_V1.1.001_130219,255,001,255,001,0,100,100,0,internet,0000,0000,0,0,255,0,4,1,11,00)]"));
        
        verifyNull(decoder, buffer(
                "[\u004c0000001323\u004e(GSM,0,0,07410001,20120101162600,404,010,9261,130,0,2353,130,35,9263,130,33,1)]"));

        verifyNull(decoder, buffer(
                "[\u00250000000082\u001d(100100000000000600-30-65535)]"));

        verifyNull(decoder, buffer(
                "[\u00230000000004\u0018(062100000000000600-0-0)]"));

        verifyPosition(decoder, buffer(
                "[\u003d0000000083\u0036(ITV013939A4913.8317N02824.9241E000.90018031310010000)]"));
        
        verifyPosition(decoder, buffer(
                "[\u003d0000000036\u0036(ITV012209A4913.8281N02824.9258E000.32018031310010000)]"));
        
        verifyPosition(decoder, buffer(
                "[\u003b0000000010\u0036(ONE200834A5952.8114N01046.0832E003.93212071305010000)]"));

        verifyPosition(decoder, buffer(
                "[\u00930000000000\u0046(ITV153047A1534.0805N03233.0888E000.00029041500000400&Wsz-wl001&B0000)]"));

    }

}
