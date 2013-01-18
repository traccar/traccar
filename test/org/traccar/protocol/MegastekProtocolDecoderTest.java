package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class MegastekProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MegastekProtocolDecoder decoder = new MegastekProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "STX,102110830074542,$GPRMC,114229.000,A,2238.2024,N,11401.9619,E,0.00,0.00,310811,,,A*64,F,LowBattery,imei:012207005553885,03,113.1,Battery=24%,,1,460,01,2531,647E;57"));

    }

}
