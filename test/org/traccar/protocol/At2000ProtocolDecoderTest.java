package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

import static org.junit.Assume.assumeTrue;

public class At2000ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        assumeTrue(Boolean.parseBoolean(System.getenv("CI")));

        At2000ProtocolDecoder decoder;

        decoder = new At2000ProtocolDecoder(new At2000Protocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "01012f0000000000000000000000000000333537343534303731363035353033dd529a1eb5df9f3b6d320b38250e03306692957e8c2127d8e381a717f639b4c9"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "89898701000000000000000000000000ae99e38f13d44f536769eb4930a6826dfebe5b98a6048941e89b17c9cdcb276be4af7c0d188d07c90d6e94aa9efcb465fe7aeaff4d85caf837483b4e9b32fbbacfbc4e175eebf57a27f552a64fc3419565d2dfbea668511a08d5a526342fad0e93b20c4449ad8defab4a9ac68cf7dad86971eb2cd96810d9d6a9c56e07fd90e4c28cfc53a069b63efe37a0523a69b607a2dc011ba17b177c5332c04be1faeeabed24539b3b790fa8a8610ab3633e0140ed79690fcae9dea43c7daad780d95a511d8f4875e621bcfe7516a03b80eb3c473ffd4bc1eda298dfa7d994a2cfeaa5d24c190d52d72fd90975a2e6f9ed3b95017133952262f91787c46839738a80c333dc53ee4d8afe75315d801efe17bc7309f30cfce64906bf70e6844c835781cbb64b49e9315ca3c2cd39d00a03cc7178a4ebc5df230dcdfd44ec588791d488f96bb6ff4007a753f552bda4d1766632aa3ec5eb38feb23ed6efb8f382a7f22b70adc9cb533c09bf749190c36d63b572c1acfc3a59138d51273835ab13c4689df01e3d2c2dd1829e00aac5c56b5d51e60d6731833f82c7464d88df663ca28a20eedeecb60f3704ae78281838caa116184e414db459768321bbfa1e83ad59fe168eb81f3b41cfe0e39c8aa78cbbe5825620bf053a1cb62e04d4cdf17ca2dc9305d47c"));

        decoder = new At2000ProtocolDecoder(new At2000Protocol());

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
