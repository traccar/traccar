package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ItsProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        ItsProtocolDecoder decoder = new ItsProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$,04,XYZ123,0.0.1,TA,16,L,861359034100626,MH12AB1234,1,14,10,2018,04,50,52,018.489624,N,073.855980,E,000.0,039.86,13,584.1,01.11,00.75,AIRTEL,1,1,00.0,4.1,1,C,15,404,90,0c23,781a,0000,0000,0000,0000,0000,0000,0000,0000,0000,0000,0000,0000,1000,01,000005,13b75499,"));

        verifyNull(decoder, text(
                "$,01,XYZ123,0.0.1,861359034137271,MH12AB1234,"));

        verifyNull(decoder, text(
                "$,02,XYZ123,0.0.1,861359034137271,100,30,00.0,00005,00600,1000,01,00.1,00.0,"));

        verifyPosition(decoder, text(
                "$,EPB,EMR,861359034100626,SP,00,00,0000,00,00,00,V,000.000000,N,000.000000,E,000.0,000.0,000.00,N,MH12AB1234,0000000000000,d34679e1,"));

        verifyPosition(decoder, text(
                "$,03,XYZ123,0.0.1,TA,16,L,861359034137271,MH12AB1234,0,00,00,0000,00,00,00,000.000000,N,000.000000,E,000.0,000.00,00,000.0,00.00,00.00,IDEAIN,1,1,00.0,4.0,1,O,16,404,22,2797,11b7,11b9,2797,-087,11b8,2797,-093,11b4,2797,-106,0000,0000,0000,1000,01,000032,8173e058,"));

        verifyPosition(decoder, text(
                "$,04,XYZ123,0.0.1,BR,06,L,861359034137271,MH12AB1234,0,00,00,0000,00,00,00,000.000000,N,000.000000,E,000.0,000.00,00,000.0,00.00,00.00,IDEAIN,1,1,00.0,3.8,1,O,17,404,22,2797,11b7,11b9,2797,-093,11b8,2797,-098,0000,0000,0000,0000,0000,0000,1000,00,000006,abd26284,"));

    }

}
