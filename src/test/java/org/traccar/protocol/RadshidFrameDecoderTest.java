package org.traccar.protocol;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class RadshidFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 4096, 0, 4, 0, 0, true));

        decoder.writeInbound(binary(
                "600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a"));

        verifyFrame(
                binary("600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a"),
                decoder.readInbound());

    }

}
