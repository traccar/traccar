package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class OkoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new OkoProtocolDecoder(null));

        verifyPosition(decoder, text(
                "{861001001012919,090745,A,4944.302,N,02353.366,E,0.0,225,251120,7,0.27,F9,11.3,1}"));

        verifyPosition(decoder, text(
                "{868204000482330,125138,A,5026.821,N,03032.472,E,0.0,171,240200,7,00,F9,7D,1,,,,,,,91,,,187.7,M,2,,}"));

        verifyPosition(decoder, text(
                "{123456789098765,132810.000,A,4926.4243,N,03203.6831,E,0.08,83.52,131010,07,5C,FB,7A,1,27,,,,,,CB,128,15grn,197.6,M,3,01FE,02AC}"));

        verifyPosition(decoder, text(
                "{861694033681089,045403.00,A,4924.14181,N,03207.43787,E,0.080,,151117,07,0.00,01,24.8,1,02,5n4}"));

        verifyPosition(decoder, text(
                "{045411.00,A,4924.14243,N,03207.43754,E,0.172,,151117,07,0.00,F9,28.1,2,C2,5n4}"));

        verifyPosition(decoder, text(
                "{861001001016415,115031.000,A,4804.101180,N,02255.227002,E,4.121,111.0,211215,6,0.00,F7,13.6,1,00"));

        verifyPosition(decoder, text(
                "{132810.000,A,4926.4243,N,03203.6831,E,25.0,183,131011,07,5.69,05,14.1,1,82,3N5"));

        verifyPosition(decoder, text(
                "{115034.000,A,4804.098944,N,02255.233436,E,7.858,120.9,211215,7,0.00,F7,13.7,1,00"));

        verifyPosition(decoder, text(
                "{115038.000,A,4804.091227,N,02255.250213,E,17.621,128.1,211215,8,0.00,00,13.7,2,00"));

    }

}
