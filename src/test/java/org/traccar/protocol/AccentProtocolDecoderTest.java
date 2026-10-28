package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccentProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AccentProtocolDecoder(null));

        verifyNull(decoder, text(
                "AA011.0.103R10, ICC:8921602050440128136F, IMEI:861001002935274"));

        verifyNull(decoder, text(
                "860141077743871 AA00124427@14/01/27,12:44:20+00"));

        verifyNull(decoder, text(
                "AAAA"));

        verifyNull(decoder, text(
                "860141077743871 AA01MF2"));

        verifyNull(decoder, text(
                "860141077743871 AA01MF2_XS"));

        verifyPosition(decoder, text(
                "860141077743871 AA23010279022CB3190099432C0011122A18FFFFFFFF800000000000010000000026D70B14"),
                position("2026-09-24 18:22:49.000", true, 36.80644, 10.07367));

        verifyAttribute(decoder, text(
                "860141077743871 AA23010279022CB3190099432C0011122A18FFFFFFFF800000000000010000000026D70B14"),
                Position.KEY_BATTERY, 4.2);

        verifyPosition(decoder, text(
                "861001002935274 AA2101171A022D833D009A2BAA0000A0000000006380FC000000000400000000001458"));

        verifyPosition(decoder, text(
                "861001002935274 AA1300ADCA01F0CE94009CB42D0000212A00FD01FEE3801300000000071739875B1B8C"
                        + "0000000000000000000000000000000C1A"));

    }

    @Test
    public void testMultiFramePacket() throws Exception {

        var frames = new EmbeddedChannel(
                new CharacterDelimiterFrameDecoder(1024, "\r\n", "\n", "\r"));
        var decoder = inject(new AccentProtocolDecoder(null));

        frames.writeInbound(buffer(
                "\n\r\nAAAA\r\n\n\r\n",
                "AA3300AF34022CB38C00994290000D1E2A000001FE67800000000000030000000026FA",
                "000000000000000000000000000000091C\r\n",
                "AA2300AEE5022CB3BB009942140010222A0000000067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "AA2300AEE8022CB3B10099420100111E2A00FF010067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "AA2300AEEA022CB3A00099420D00100D2A00FF000067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "AA2300AEED022CB3800099423D0010092A00FF010067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "AA2300AEEF022CB37600994251000B192A00FF000067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "AA2300AEF1022CB37400994260000C222A00FE010067800000000000030000000026FA",
                "000000000000000000000000000000071A\r\n",
                "\n\r\nAAAA\r\n\n\r\n"));

        int positions = 0;
        ByteBuf frame;
        while ((frame = frames.readInbound()) != null) {
            String text = frame.toString(StandardCharsets.US_ASCII);
            frame.release();
            if (text.length() != 104) {
                continue;
            }
            Object decoded = decoder.decode(null, null, "860141077743871 " + text);
            if (decoded instanceof Position) {
                positions++;
            }
        }
        assertEquals(7, positions);

    }

}
