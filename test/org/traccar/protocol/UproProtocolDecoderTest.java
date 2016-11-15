package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class UproProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        UproProtocolDecoder decoder = new UproProtocolDecoder(new UproProtocol());

        verifyPosition(decoder, text(
                "*MG201693502000035441,BA&A1213073325458307036690710000151116&P0730000032ce4fb3&D1&B0000000000&C005799?7&S3,20161115120025,07035.54659E,3324.87721N,3000,0,0,0,0,847,599,8,40,0,19,20&U_P\0\0\0\0\0\0\0\0\0\0\0\0\0\0&T0107#"));

        verifyPosition(decoder, text(
                "*MG201693502000034964,AB&A0800253335360507036975710000091116&P0730000032d2a94d&B0000000000&N13&Z12&U_P\0\0\0\u0004\0\0\0\0\0\0\0\0\0\0#"),
                position("2016-11-09 08:00:25.000", true, -33.58934, -70.61626));

        verifyNothing(decoder, text(
                "*MG20113800138000,AH#"));

        verifyPosition(decoder, text(
                "*MG201693502000034964,AB&A0200183324418107033792800009051116&B0000000000&N15&Z94&U_P\0\0\0\0\0\0\0\0\0\0\0\0\0\0#"));

        verifyPosition(decoder, text(
                "*MG201693502000034964,AB&A0200543324412007033805910000051116&P0730000032d66785&B0000000000&N15&Z92&U_P\0\0\0\0\0\0\0\0\0\0\0\0\0\0#"));

        verifyPosition(decoder, text(
                "*AI2000905447674,BA&A2003064913201201845107561627121016&B0100000000&C05>8=961&F0333&K023101002154A7#"));

        verifyPosition(decoder, text(
                "*AI200905300036,AH&A0317264913209801844913060000251115&B0500000000&C0;4?72:9&F0000#"),
                position("2015-11-25 03:17:26.000", false, 49.22016, 18.74855));

        verifyPosition(decoder, text(
                "*AI2000905300036,AS&A1647304913209801844913060000251115&B0400000000&C0;4?72:9&F0000"));

        verifyPosition(decoder, text(
                "*AI2000905300036,AC1&A1648014913209801844913060000251115&B0400000000&C0;4?72:9&F0000"));

        verifyPosition(decoder, text(
                "*AI2000905300036,AB1&A1702464913231101844949860000251115&B0500000000&C0;4?72:9&F0000#"));

        verifyPosition(decoder, text(
                "*AI2000905300036,AD1&A1703054913231101844949860000251115&B0500000000&C0;4?72:9&F0000"));

    }

}
