package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PuiProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PuiProtocolDecoder(null));

        verifyNull(decoder, MqttMessageBuilders.connect().clientId(
                "123456789012345").build());

        verifyPosition(decoder, MqttMessageBuilders.publish().payload(buffer(
                "{ \"id\": \"015262001044848\", \"ts\": \"2023-06-01T03:09:51.362Z\", \"rpt\": \"hf\", \"location\": { \"lat\": 33.91233, \"lon\": -84.20784 }, \"bear\": 70, \"spd\": 2482, \"ign\": \"on\" }")).qos(MqttQoS.EXACTLY_ONCE).messageId(1).build());

    }

}
