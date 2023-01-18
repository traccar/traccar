package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class GnxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new GnxProtocolDecoder(null));

        verifyPosition(decoder, text(
                "$GNX_MIF,865733022354161,143,0,172642,180316,172642,180316,1,13.034581,N,080.234521,E,0,05396274,ROUTE_2#########,Deo ############,GNX04008,B0*"));

        verifyPosition(decoder, text(
                "$GNX_LOC,865733022352132,095,0,102134,280914,102134,280914,1,18.765432,N,073.752811,W,032,165.32,12,25,0,A,E,2,000099.9,000099.5,GNX01001,12*"));

        verifyNull(decoder, text(
                "$GNX_LOC,865733022354161,139,0,142838,160316,142825,160316,0,000000000,N,0000000000,E,000,0.00,00,48,0,e,C,2,000000.0,000000.0,GNX04008,BB*"));

        verifyPosition(decoder, text(
                "$GNX_DIO,863071015071563,110,1,155627,121214,151244,121214,1,08.878321,N,076.643154,E,0,0,0,0,0,0,GNX01001,B1*"));

        verifyNull(decoder, text(
                "$GNX_DIO,865733022354161,112,1,142849,160316,142714,160316,0,000000000,N,0000000000,E,0,0,0,0,0,0,0,GNX04008,1A*"));

    }

}
