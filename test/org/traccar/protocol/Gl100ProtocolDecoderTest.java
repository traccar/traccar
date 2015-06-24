package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Gl100ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gl100ProtocolDecoder decoder = new Gl100ProtocolDecoder(new Gl100Protocol());

        assertNull(decoder.decode(null, null,
                "AT+GTHBD=HeartBeat,359231030000010,20090101000000,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTSOS,359231030000010,0,0,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTRTL,359231030000010,0,0,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTEST,359231030000010,0,0,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTSZI,359231030000010,0,3,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTLBC,359231030000010,02132523415,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTTRI,359231030000010,1,0,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTTRI,359231030000010,2,0,0,1,4.3,92,70.0,1,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,0,0,1,-3.6,145,30.0,2,121.354442,31.221940,20090101000100,0460,0000,18d8,6141,00,11F0,0102120204"));

        verify(decoder.decode(null, null,
                "+RESP:GTTRI,359464030073766,1,0,0,0,1.7,254,-27.8,3,30.474475,50.488383,20131107155511,0255,0003,6995,4761,00,0071,0103090402"));

    }

}
