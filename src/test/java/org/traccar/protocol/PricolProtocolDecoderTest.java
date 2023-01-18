package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PricolProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PricolProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "3c5052493030303350020000011402110b222b0455152e4e001de819ca450000000000000003820249000000000000000000000000000000000000000040003e"));

        verifyNotNull(decoder, binary(
                "3c544553303030324b02000000000000000000000000000000000000000000000000000000037c01f4000000000000000000000000000000000000000000003e"));

        verifyPosition(decoder, binary(
                "3c4944303030303150FFFFFFFF1C050C121D38045D09FA4e001DE815F4452FFFFFFFFFFF03FF03FF03FF03FF03FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF113e"));

    }

}
