package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class IotmProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        IotmProtocolDecoder decoder = new IotmProtocolDecoder(null);

        verifyNull(decoder, MqttMessageBuilders.connect().clientId(
                "123456789012345").build());

        verifyPositions(decoder, false, MqttMessageBuilders.publish().payload(binary(
                "020208009188752DE7120300030A002000AD59000146543250030A002200AD59643346543250FB")).qos(MqttQoS.EXACTLY_ONCE).messageId(1).build());

        verifyPositions(decoder, false, MqttMessageBuilders.publish().payload(binary(
                "020208004f6af053901403000135007715ec5f0d02b000485cea73083041040b30ee050030ae38040020120e00d0d34bc9412e9c5d4212000806ee00feff011300006d006e")).qos(MqttQoS.EXACTLY_ONCE).messageId(1).build());

        verifyPositions(decoder, false, MqttMessageBuilders.publish().payload(binary(
                "020208009188752DE7120300013A002000AD59050030B135030340030C300301A00302A00E00D0B9AB5B420334C04100001F060000320004072064008C000162002000C004476F6F440109002100AD59050030BA359B")).qos(MqttQoS.EXACTLY_ONCE).messageId(1).build());

        verifyPositions(decoder, false, MqttMessageBuilders.publish().payload(binary(
                "020208009188752DE71203000109002000AD590500309635F3")).qos(MqttQoS.EXACTLY_ONCE).messageId(1).build());

    }

}
