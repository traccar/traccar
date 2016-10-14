package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class UproProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        UproProtocolDecoder decoder = new UproProtocolDecoder(new UproProtocol());

        verifyNothing(decoder, text(
                "*AI2000905447674,BA&A2003064913201201845107561627121016&B0100000000&C05>8=961&F0333&K023101002154A7#"));

        verifyPosition(decoder, text(
                "*AI200905300036,AH&A0317264913209801844913060000251115&B0500000000&C0;4?72:9&F0000#"));

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
