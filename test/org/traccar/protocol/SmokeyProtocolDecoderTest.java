package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class SmokeyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        SmokeyProtocolDecoder decoder = new SmokeyProtocolDecoder(new SmokeyProtocol());

        verifyNothing(decoder, binary(
                "534d0300865628025163272f031400000000001c000200000c0168028f000102c9f93a011f538d"));

        verifyNothing(decoder, binary(
                "534d0300865628025163272f031400000000001f000200000c0167028f000102c9f93a011f5082"));

        verifyNothing(decoder, binary(
                "534d0300865628025163272f031400000000001d000200000c0167028f000102c9f93a011f5282"));

        verifyNothing(decoder, binary(
                "534d0300865628025163272f031400000000001e000200000c0167028f000102c9f93a011f5182"));

    }

}
