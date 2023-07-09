package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class MoovboxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MoovboxProtocolDecoder(null));

        verifyPositions(decoder, request(HttpMethod.POST, "/",
                buffer("<gps id=\"911\">\n<coordinates><coordinate>\n<fix>3</fix>\n<time>1597580050</time>\n<latitude>100.726257</latitude>\n<longitude>13.821351</longitude>\n<altitude>9.500000</altitude>\n<climb>0.000000</climb>\n<speed>0.064000</speed>\n<separation>-27.300000</separation>\n<track>0.000000</track>\n<satellites>9</satellites>\n</coordinate></coordinates>\n</gps>")));

    }

}
