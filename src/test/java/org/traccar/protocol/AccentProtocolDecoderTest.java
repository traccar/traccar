package org.traccar.protocol;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AccentProtocolDecoderTest extends ProtocolTest {

    private static final String GPS_FRAME =
            "AA1300ADCA01F0CE94009CB42D0000212A00FD01FEE3801300000000071739875B1B8C0000000000000000000000000000000C1A";

    private static final String LIVE_FRAME =
            "AA1300BF8A020C4E57009DFDB90000192A19FE01FEE3800000000000010000000026D711280003E0550003B70000000000000C11";

    private static final String TAXI_FRAME =
            "AA13010D2F03BB6EE103BB6EF8201602AA0004FC03A72016000000000F0000000004560000000000000000000000000000000B0F";

    @Test
    public void testDecode() throws Exception {
        var decoder = inject(new AccentProtocolDecoder(null));

        verifyAttribute(decoder, text("1001 " + GPS_FRAME), Position.KEY_IGNITION, false);
        verifyAttribute(decoder, text("1001 " + GPS_FRAME), "course", 33.0 * 16);
        verifyAttribute(decoder, text("1001 " + GPS_FRAME), Position.KEY_SATELLITES, 12);
        verifyAttribute(
                decoder, text("\n" + LIVE_FRAME.substring(0, 40) + "\n\n" + LIVE_FRAME.substring(40) + "\n"),
                "course", 25.0 * 16);
        verifyAttribute(decoder, text(LIVE_FRAME), "course", 25.0 * 16);
        verifyAttribute(decoder, text(LIVE_FRAME), Position.KEY_BATTERY, 4.2);
        verifyAttribute(decoder, text(LIVE_FRAME), Position.KEY_BATTERY_LEVEL, 100);

        verifyAttribute(decoder, text("1001 " + GPS_FRAME.substring(0, 74)), Position.KEY_IGNITION, false);

        verifyAttribute(decoder, text("1001 " + TAXI_FRAME), "sendFlag", 15);
        verifyAttribute(decoder, text("1001 " + TAXI_FRAME), "tripId", 1110);

        verifyNull(decoder, text("$GPRMC,000000,A,0000.0000,N,00000.0000,E,0.0,0.0,010120,0.0,W"));
        verifyNull(decoder, text("1001 T01" + GPS_FRAME.substring(3)));
        verifyNull(decoder, text("1001 TX1" + GPS_FRAME.substring(3)));
        verifyNull(decoder, text("1 AA00"));
        verifyNull(decoder, text("860141077743871 AA01MF2"));
        verifyNull(decoder, text("860141077743871 AA01MF2_XS"));
        verifyNull(decoder, text("AA3" + GPS_FRAME.substring(3)));
        verifyNull(decoder, text("1001 AA3" + GPS_FRAME.substring(3)));
        verifyNull(decoder, text("AAAA"));
    }

    @Test
    public void testAck() throws Exception {
        var decoder = inject(new AccentProtocolDecoder(null));
        Channel channel = mock(Channel.class);
        decoder.decode(channel, new InetSocketAddress("127.0.0.1", 5268), "\nAA03000\n\nAAAA\n\n");

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(channel).writeAndFlush(captor.capture());
        assertEquals("AA06\r\n", ((NetworkMessage) captor.getValue()).getMessage());
    }

}
