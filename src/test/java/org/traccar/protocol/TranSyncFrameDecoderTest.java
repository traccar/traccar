package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TranSyncFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TranSyncFrameDecoder());

        verifyFrame(
                binary("3a3a2b003d086256707324962001571019031c063824016604c60856f93000015f2df8530700000f1f2a310000000105000001628802050000000000050225e02323"),
                decoder.decode(null, null, binary("3a3a2b003d086256707324962001571019031c063824016604c60856f93000015f2df8530700000f1f2a310000000105000001628802050000000000050225e02323")));

    }

}
