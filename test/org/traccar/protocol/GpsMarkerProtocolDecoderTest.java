package org.traccar.protocol;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class GpsMarkerProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GpsMarkerProtocolDecoder decoder = new GpsMarkerProtocolDecoder(new GpsMarkerProtocol());


        assertNull(decoder.decode(null, null,
                "$GM300350123456789012T100511123300G25000001772F185200000000000000005230298#"));

        verify(decoder.decode(null, null,
                "$GM200350123456789012T100511123300N55516789E03756123400000035230298#"));

        verify(decoder.decode(null, null,
                "$GM1350123456789012T1005111233N55516789E03756123400000035200298#"));

        verify(decoder.decode(null, null,
                "$GM203863071014445404T150715202258N55481576E03729275300000040530301#"));

    }

}
