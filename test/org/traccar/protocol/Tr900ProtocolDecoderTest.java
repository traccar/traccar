package org.traccar.protocol;

import org.junit.Test;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class Tr900ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tr900ProtocolDecoder decoder = new Tr900ProtocolDecoder(null);

        verify(decoder.decode(null, null,
                ">12345678,1,1,070201,144111,W05829.2613,S3435.2313,,00,034,25,00,126-000,0,3,11111111*2d!"));

    }

}
