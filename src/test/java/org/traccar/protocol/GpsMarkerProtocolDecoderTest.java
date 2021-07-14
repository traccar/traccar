package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;


public class GpsMarkerProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new GpsMarkerProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$GM23D863071014445404T260816142611N55441051E037325071033063C0530304#"));

        verifyNull(decoder, text(
                "$GM300350123456789012T100511123300G25000001772F185200000000000000005230298#"));

        verifyPosition(decoder, text(
                "$GM200350123456789012T100511123300N55516789E03756123400000035230298#"),
                position("2011-05-10 12:33:00.000", true, 55.86132, 37.93539));

        verifyPosition(decoder, text(
                "$GM1350123456789012T1005111233N55516789E03756123400000035200298#"));

        verifyPosition(decoder, text(
                "$GM203863071014445404T150715202258N55481576E03729275300000040530301#"));

    }

}
