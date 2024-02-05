package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class LacakProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new LacakProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"location\":{\"event\":\"motionchange\",\"is_moving\":false,\"uuid\":\"0e9a2473-a9a7-4c00-997b-fb97d2154e75\",\"timestamp\":\"2021-07-21T08:06:34.444Z\",\"odometer\":0,\"coords\":{\"latitude\":-6.1148096,\"longitude\":106.6837015,\"accuracy\":3.8,\"speed\":18.67,\"speed_accuracy\":0.26,\"heading\":63,\"heading_accuracy\":0.28,\"altitude\":35.7,\"altitude_accuracy\":3.8},\"activity\":{\"type\":\"still\",\"confidence\":100},\"battery\":{\"is_charging\":false,\"level\":0.79},\"extras\":{}},\"device_id\":\"8737767034\"}")));

    }

}
