package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class DishaProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new DishaProtocolDecoder(null));

        verifyPosition(decoder, text(
                "$A#A#864161028848856#A#053523#010216#2232.7733#N#08821.1940#E#002.75#038.1#09#00.8#1800#0#000#0000#9999#11.7#285.7#0001*"));

        verifyPosition(decoder, text(
                "$A#A#864161028848856#A#182134#090116#2232.0191#N#08821.3278#E#001.74#231.4#04#01.5#1300#0#000#0000#9999#54.4#6407.7#0000*"),
                position("2016-01-09 18:21:34.000", true, 22.53365, 88.35546));

        verifyPosition(decoder, text(
                "$A#A#353943046615971#A#064219#281113#1836.7267#N#07347.4177#E#000.00#280.4#09#00.8#3000#2#100#0000#8888#86.5#3919.1#0000*"));

    }

}
