package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TmgProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TmgProtocolDecoder decoder = new TmgProtocolDecoder(new TmgProtocol());

        verifyPosition(decoder, text(
                "$nor,L,868324023777431,17012017,001023,4,2830.2977,N,07705.2478,E,0.0,207.07,229.2,0.97,11,22,IDEA CELLULAR L,18,DCDE,0,4.09,12.9,00000111,00000000,1111,00.0-00.0,00.0-0.0,3.59,01.02,#"));

        verifyPosition(decoder, text(
                "$nor,L,868324023777431,17012017,001523,4,2830.2939,N,07705.2527,E,0.0,50.96,236.5,1.05,11,21,IDEA CELLULAR L,18,DCDE,0,4.09,12.8,00000111,00000000,1111,00.0-00.0,00.0-0.0,3.59,01.02,#"));

        verifyPosition(decoder, text(
                "$nor,L,869309999985699,24062015,094459,4,2826.1956,N,07659.7690,E,67.5,2.5,167,0.82,15,22,airtel,31,4441,1,4.1,12.7,00000011,00000011,1111,0.0,0.0, 21.3,SW00.01,#"));

    }

}
