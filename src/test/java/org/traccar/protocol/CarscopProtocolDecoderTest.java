package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class CarscopProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new CarscopProtocolDecoder(null));

        verifyNull(decoder, text(
                "*170821223045UB00HSO"));

        verifyPosition(decoder, text(
                "*170821223121UB05ORANGE000731512061825V0000.0000N00000.0000E000.0040331309.62"));

        verifyPosition(decoder, text(
                "*170724163029UB05ORANGE000000010061825V0000.0000N00000.0000E000.0040331309.62"));

        verifyNull(decoder, text(
                "*160618233129UB00HSO"));

        verifyNull(decoder, text(
                "*160618232614UD00232614A5009.1747N01910.3829E0.000160618298.2811000000L000000"));

        verifyNull(decoder, text(
                "*160618232529UB05CW9999C00000538232529A5009.1747N01910.3829E0.000160618298.2811000000L000000"));

        verifyPosition(decoder, text(
                "*040331141830UB05123456789012345061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"),
                position("2004-03-31 06:18:25.000", true, 29.56689, 106.45424));

        verifyPosition(decoder, text(
                "*040331141830UB04999999984061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"));

        verifyPosition(decoder, text(
                "*040331141830UA012Hi-jack061825A2934.0133N10627.2544E000.0040331309.6200000000L000000"));

        verifyPosition(decoder, text(
                "*150817160254UB05CC8011400042499160254A2106.8799S14910.2583E000.0150817158.3511111111L000000"));

    }

}
