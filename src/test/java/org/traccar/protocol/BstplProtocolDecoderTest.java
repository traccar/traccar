package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class BstplProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new BstplProtocolDecoder(null));

        verifyPosition(decoder, text(
                "BSTPL$1,861409078693513,A,090525,124513,17.348024,N,78.515709,E,0,0,000,00,20,0,04.16,1,1,11.84,0000,0,17L_LDM_V1_0,8991000921993669778F,{0,0,0,0,0.00,0.00}"));

        verifyPosition(decoder, text(
                "BSTPL$1,869630054439504,V,200722,045113,00.000000,0,00.00000,0,0,0,000,00,0,17,1,1,0,0,00.01,0,04.19,15B_190821,8991000907387031196F,12.27"));

        verifyPosition(decoder, text(
                "BSTPL$1,AP12AP3456,A,130720,160552,27.244183,N,83.673973,E,20,156,183,17,0,11,1,0,0,0,00.00,00,04.16,15_V1_0_0,89917380578146790443,12.16"));

    }

}
