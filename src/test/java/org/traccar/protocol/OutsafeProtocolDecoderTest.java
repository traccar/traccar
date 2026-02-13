package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class OutsafeProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new OutsafeProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"device\":\"865303040103725\",\"owner\":\"\",\"data\":{\"cmd\":\"\",\"ms1\":-1,\"ms2\":-1,\"ms3\":0,\"ms4\":0,\"observation\":\"\",\"content\":null},\"time\":1589277568,\"origin\":\"mqgatte\",\"latitude\":19.346855,\"longitude\":-99.29587,\"altitude\":2757,\"heading\":0,\"rssi\":0}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"device\":\"862061044762093\",\"owner\":\"\",\"data\":{\"cmd\":\"GEO\",\"ms1\":82,\"ms2\":80,\"ms3\":5266,\"ms4\":-68,\"observation\":\"$NMEA 323455\",\"content\":null},\"time\":null,\"origin\":\"TCP\",\"latitude\":19.334734,\"longitude\":-99.307236,\"altitude\":2000,\"heading\":0,\"rssi\":123}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"device\":\"1e09d88a-fe8e-4dee-90b9-6297088ff3de\",\"owner\":\"\",\"data\":{\"cmd\":\"GEO\",\"ms1\":82,\"ms2\":80,\"ms3\":5266,\"ms4\":-68,\"observation\":\"$NMEA 323455\",\"content\":null},\"time\":null,\"origin\":\"TCP\",\"latitude\":19.334734,\"longitude\":-99.307236,\"altitude\":2000,\"heading\":0,\"rssi\":123}")));

    }

}
