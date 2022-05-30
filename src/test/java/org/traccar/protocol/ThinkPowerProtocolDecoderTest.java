package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ThinkPowerProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ThinkPowerProtocolDecoder(null));

        verifyNull(decoder, binary(
                "0103002C01020F38363737333030353038323030343606544C3930344111522D312E302E31372E32303231303431300011C3"));

        verifyPosition(decoder, binary(
                "05300012016099E995010D743CC943EB481500000000EED4"));

        verifyPosition(decoder, binary(
                "05000007016099E768020162D8"));

        verifyNull(decoder, binary(
                "03040000C3DC"));

    }

}
