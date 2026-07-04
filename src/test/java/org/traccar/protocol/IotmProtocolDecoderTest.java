package org.traccar.protocol;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.junit.jupiter.api.Test;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;

import java.net.SocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class IotmProtocolDecoderTest extends ProtocolTest {

    private MqttMessage decodeMqtt(String data) {
        var channel = new EmbeddedChannel(new MqttDecoder());
        channel.writeInbound(binary(data));
        return channel.readInbound();
    }

    private void verifyMqttMessage(String expected, MqttMessage message) {
        var channel = new EmbeddedChannel(MqttEncoder.INSTANCE);
        channel.writeOutbound(message);
        verifyFrame(binary(expected), channel.readOutbound());
    }

    @Test
    public void testTelemetryTopic() throws Exception {

        var decoder = inject(new IotmProtocolDecoder(null));

        verifyNull(decoder, MqttMessageBuilders.connect().clientId("869595060875258").build());

        String outputOff =
                "327200054243452f44000202020800faafb360e4160300015900ab4b306a0302b0030b30050030eb2e"
                        + "0504307a0c030340030140030240031d30030c300301a0031ba00300200e00d0e0402d42ca56dd4100"
                        + "00090e000082010094000095000401200e04052035001300000200030820030720fb";

        MqttPublishMessage message = assertInstanceOf(MqttPublishMessage.class, decodeMqtt(outputOff));

        assertEquals("BCE/D", message.variableHeader().topicName());
        assertEquals(2, message.variableHeader().packetId());
        verifyPositions(decoder, false, decodeMqtt(outputOff));
        verifyAttribute(decoder, decodeMqtt(outputOff), "out1", false);
        verifyAttribute(decoder, decodeMqtt(outputOff), "out2", false);

        String outputOn =
                "327200054243452f44000202020800faafb360e4160300015900c34b306a0302b0030b30050030eb2e"
                        + "050430800c030340030140030240031d30030c300301a0031ba00300200e00d0e0402d42ca56dd4100"
                        + "000b0e000082010194000095000401200e040520350013000002000308200307201c";

        verifyPositions(decoder, false, decodeMqtt(outputOn));
        verifyAttribute(decoder, decodeMqtt(outputOn), "out1", true);
        verifyAttribute(decoder, decodeMqtt(outputOn), "out2", false);
    }

    @Test
    public void testPingRequest() throws Exception {

        var decoder = inject(new IotmProtocolDecoder(null));
        var channel = new EmbeddedChannel(decoder);

        channel.writeInbound(new NetworkMessage(decodeMqtt("c000"), mock(SocketAddress.class)));

        boolean pingResponse = false;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof NetworkMessage networkMessage) {
                var message = assertInstanceOf(MqttMessage.class, networkMessage.getMessage());
                assertEquals(MqttMessageType.PINGRESP, message.fixedHeader().messageType());
                verifyMqttMessage("d000", message);
                pingResponse = true;
            }
        }

        assertTrue(pingResponse);
        verifyNull(channel.readInbound());
    }

}
