package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class PluginProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new PluginProtocolDecoder(null);

        verifyPosition(decoder, text(
                "$$STATUS,000000900005,20210521111252,27.171105,-25.600934,62.0,323,0,-1,2,0.000,2147489155,0.00,0,0,0.0,0.0,0,0,0,0,0,0,0,0,0"));

        verifyAttribute(decoder, text(
                "$$STATUS,60925,20190829123115,28.254151,-25.860605,0.0,0,0,-1,2,0.000,13699,0.00,0,0,28.4,23.4,0,0,0,0,0,0,0,0,0"),
                Position.PREFIX_TEMP + 1, 28.4);

        verifyPosition(decoder, text(
                "$$STATUS,60550,20191014084650,28.254258,-25.860355,0.0,236,0,-1,2,7472.967,13697,0.00,0,0,0.0,0.0,0,0,0,0,0,0,0,0,0"));

        verifyPosition(decoder, text(
                "$$STATUS,fleet40,20190704122622,26.259431,-29.027889,0,9,0,-1,2,19719,805315969,0,0,0"));

        verifyPosition(decoder, text(
                "$$ALARM801739,20190612121950,28.254067,-25.860494,0,0,0,-1,2,2,12595331,0,0,0,+,22,0,0,0,0,0,,0,0"));

        verifyPosition(decoder, text(
                "$$STATUS801739,20190528143943,28.254086,-25.860665,0,0,0,-1,2,78,11395,0,0,0"));

        verifyPosition(decoder, text(
                "50000,20150623184513,113.828759,22.709578,70,190,0,-1,2,155135681,805327235,1.32,-32.1,0"));

    }

}
