package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SiwiProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        SiwiProtocolDecoder decoder = new SiwiProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$SIWI,9803932,23992,E,0,,0,1,1,0,5055,0,5,A,22.289887,70.807192,152,168,102922,090317,28,1,12,5,4098,1,0,13,0,0,0,1.0,3.1CHKS_4.82,0!"));

        verifyPosition(decoder, text(
                "$SIWI,2845,1320,Q,10,airtelgprs.com,1,1,0,0,876578,43,9,A,19.0123456,72.65347,45,0,055929,071107,22,5,1,0,3700,1210,0,2500,1230,321,0,1.1,4.0,1!"));

        verifyPosition(decoder, text(
                "$SIWI,9803849,953,R,9,,0,1,1,0,0,0,8,A,19.066145,73.002278,213,178,122738,210217,28,5,12,6,4066,1,0,2,0,0,0,1.0,3.1CHKS_4.82,0"));

        verifyPosition(decoder, text(
                "$EIT,9803849,953,R,9,,0,1,1,0,0,0,8,A,19.066145,73.002278,213,178,122738,210217,28,5,12,6,4066,1,0,2,0,0,0,1.0,3.1CHKS_4.82,0"));

        verifyPosition(decoder, text(
                "$SIWI,9803849,954,E,0,,0,1,1,0,0,0,0,V,0.000000,0.000000,0,0,122855,210217,29,5,12,5,4104,1,0,2,0,0,0,1.0,3.1CHKS_4.82,0"));

        verifyPosition(decoder, text(
                "$SIWI,2845,1320,A,0,,1,1,0,0,876578,43,10,A,19.0123456,72.65347,45,0,055929,071107,22,5,1,0,3700,1210,0,2500,1230,321,0,1.1,4.0,1!"));

        verifyPosition(decoder, text(
                "$SIWI,9803849,956,E,0,,0,1,1,0,0,0,3,V,19.066935,73.003383,0,111,123037,210217,28,5,12,5,4071,1,0,2,0,0,0,1.0,3.1CHKS_4.82,0"));

    }

}
