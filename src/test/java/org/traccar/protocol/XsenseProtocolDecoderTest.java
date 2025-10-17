package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class XsenseProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new XsenseProtocolDecoder(null));

        // Real xsense packet - Type 114 (0x72) = M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO
        verifyPositions(decoder, binary(
                "72ad3ac5bd7d3faeabba63d854264aad4d4dad2dbb0f6e69abba63f8542645ad4d4dad2dbb0f6e75abba6398542647ad4d4dad2d7b0f6e41abba63f854264aad4d4dad2dbb0f69a9abba605854264bad4d4dad2dbb0f69b5abba63a8542645ad4d4dad2dbb0f6981ada4579901d9a98c52529395949b9b9c959c9d9d959f98949e9899949b98adadadadadadadadadadadadadadadadadad6269"));

    }

}
