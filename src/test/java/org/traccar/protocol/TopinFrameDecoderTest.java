package org.traccar.protocol;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.ProtocolTest;

public class TopinFrameDecoderTest extends ProtocolTest {

    @Test
    public void testMultipleFrames() throws Exception {

        var decoder = new EmbeddedChannel(
                new CharacterDelimiterFrameDecoder(1024, false, "\r\n"));

        decoder.writeInbound(binary(
                "787801300d0a"
                + "787801570d0a"
                + "787816136407020534000000000000009500181a09021527000d0a"));

        verifyFrame(
                binary("787801300d0a"),
                decoder.readInbound());

        verifyFrame(
                binary("787801570d0a"),
                decoder.readInbound());

        verifyFrame(
                binary("787816136407020534000000000000009500181a09021527000d0a"),
                decoder.readInbound());
    }

}
