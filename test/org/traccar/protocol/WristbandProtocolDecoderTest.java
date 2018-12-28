package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class WristbandProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        WristbandProtocolDecoder decoder = new WristbandProtocolDecoder(null);

        verifyNull(decoder, binary(
                "000102004759583836383730343034343735303035357c56312e307c317c7b463931233331305f30307c30307c30307c30307c57414e444149323031382f31322f31342031353a35367d0d0afffefc"));

        verifyNull(decoder, binary(
                "000102004159583336373535313631303030303934347c56312e307c317c7b4639312330317c30307c30307c33475f7065745f323031382f30352f31362031313a30307d0d0afffefc"));

        verifyNull(decoder, binary(
                "000102003559583836383730343034343735303035357c56312e307c317c7b4630312339342c312c3130302c302c33313030302c3930307d0d0afffefc"));

    }

}
