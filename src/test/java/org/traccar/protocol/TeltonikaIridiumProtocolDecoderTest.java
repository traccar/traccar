package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class TeltonikaIridiumProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TeltonikaIridiumProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "01003e01001c2004206633303035333430363734363731383000000600006abbec9503000b011389a062e12b0000000402000e6ab47e4739a4609bddf40000ffff"),
                position().location("2026-09-24T01:35:03.000Z", false, 19.593994, -98.940806));

        verifyDecode(decoder, binary(
                "01003e01001c20102bba33303035333430363734363731383000002b00006abbf87703000b01138a1162d8570000000302000e6abbf7bf39a45a9bddf70000ffff"),
                position().location("2026-09-29T17:39:11.000Z", false, 19.594026, -98.940935));

        verifyDecode(decoder, binary(
                "01004c01001cf99dd263333030323334303634303730383330000018000059282dbc03000b0036a2b8193d120000000302001c59282ba391f5abcdbfdd0180000059282ba891f5abcdbfdd01800000"),
                position().location("2017-05-26T13:20:35.000Z", true, 54.667602, 25.255757),
                position().attribute(Position.PREFIX_IN + 1, true));

    }

}
