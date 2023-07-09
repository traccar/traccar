package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Tr20ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Tr20ProtocolDecoder(null));

        verifyPosition(decoder, text(
                "%%m13,L,221221103115,N1237.2271W00801.9500,000,000,B13.1:F0.0,04020000,253,CFG:133.00|"));

        verifyPosition(decoder, text(
                "%%TR20GRANT,L,210602170135,N0951.1733W08356.7672,000,000,C80:F0,00020008,108,CFG:6980.00|"));

        verifyPosition(decoder, text(
                "%%123456789012345,A,120101121800,N6000.0000E13000.0000,0,000,0,01034802,150,[Message]"));

        verifyNull(decoder, text(
                "%%TRACKPRO01,1"));

        verifyPosition(decoder, text(
                "%%868873457748532,A,181109121248,N2237.4181E11403.2857,000,282,NA,47010000,108"));

        verifyPosition(decoder, text(
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"),
                position("2005-09-16 07:05:49.000", true, 22.68148, 113.98832));

        verifyPosition(decoder, text(
                "%%TR-10,A,050916070549,N2240.8887E11359.2994,0,000,NA,D3800000,150,CFG:resend|"));

    }

}
