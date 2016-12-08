package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

import static org.junit.Assume.assumeTrue;

public class At2000ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv("CI")));

        At2000ProtocolDecoder decoder = new At2000ProtocolDecoder(new At2000Protocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01012f0000000000000000000000000000333537343534303731363237353938d74dcd195c521a246fb00f16346c7f001919957babc40f84152b60ddeb7ab47a"));

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01012f00000000000000000000000000003335363137333036343430373439320fad981997ae8e031fe10c0ea7641903ca32c0331df467233d2a9cd886fbeef8"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "893f0000000000000000000000000000e048b1a31deba3f5dbe8877f574877e6ed4d022b6611a10d80dfc4c0c11fa8aacf4a9de61528327e2b66843dd9c5d3a7cc9ee1d9c71a34bb482145d88b4fda3e"));

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01012f00000000000000000000000000003335373435343037313632363831345612da3748bede02ea4faf04ac02f420c0ff37719eccf2864fa2b8191abf8242"));

    }

}
