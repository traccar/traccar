package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ArknavProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ArknavProtocolDecoder(null));

        verifyPosition(decoder, text(
                "358266016278447,05*827,000,L001,V,4821.6584,N,01053.8650,E,000.0,000.0,00.0,08:46:04 17-03-16,9.5A,D7,0,79,0,,,,"),
                position("2016-03-17 08:46:04.000", false, 48.36097, 10.89775));

        verifyPosition(decoder, text(
                "123456789012345,05*850,000,L001,A,2459.3640,N,12125.2958,E,000.0,224.8,00.8,07:47:26 09-09-05,9.00,D3,0,C4,1,,,,"));

        verifyPosition(decoder, text(
                "123456789012345,05*850,000,L001,A,2459.3640,N,12125.2958,E,000.0,224.8,00.8,07:47:26 09-09-05,9.00,D3,0,C4,1,,,00000000,"));

    }

}
