package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PricolProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        PricolProtocolDecoder decoder = new PricolProtocolDecoder(new PricolProtocol());

        verifyNotNull(decoder, binary(
                "3c544553303030324b02000000000000000000000000000000000000000000000000000000037c01f4000000000000000000000000000000000000000000003e"));

        verifyPosition(decoder, binary(
                "3c4944303030303150FFFFFFFF1C050C121D38045D09FA4e001DE815F4452FFFFFFFFFFF03FF03FF03FF03FF03FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF113e"));

    }

}
