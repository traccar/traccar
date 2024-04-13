package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class SnapperProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SnapperProtocolDecoder(null));

        verifyNull(decoder, binary(
                "4b0341a6b0c608000040000005000000000000007d5e14010068656c6c6f"));

        // data sample
        // {"f":"DE","t":"092304.01","d":"110813","la":"5117.6370","lo":"01655.3959","a":"00166.6","s":"","c":"","sv":"08","p":"01.6"}

    }

}
