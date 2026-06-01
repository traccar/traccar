package org.traccar.protocol;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Hyn600FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024, 5, 2, -7, 0));

        decoder.writeInbound(binary(
                "2b5250543a0025560a4c081634280222030200020000000400000007ea05060e2919046123"));

        verifyFrame(
                binary("2b5250543a0025560a4c081634280222030200020000000400000007ea05060e2919046123"),
                decoder.readInbound());

        decoder.writeInbound(binary(
                "2b5250543a0068560a4c08163428022203020e020000011e0800000001ff0100000000000000000000000000000000000000000000000d0000000000000000000000000000000004a504d12200000000000000000000000000000000000007ea05060e2919046223"));

        verifyFrame(
                binary("2b5250543a0068560a4c08163428022203020e020000011e0800000001ff0100000000000000000000000000000000000000000000000d0000000000000000000000000000000004a504d12200000000000000000000000000000000000007ea05060e2919046223"),
                decoder.readInbound());

    }

}
