package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OmnicommFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        OmnicommFrameDecoder decoder = new OmnicommFrameDecoder();

        verifyFrame(
                binary("c080080061a61915340100001dec"),
                decoder.decode(null, null, binary("c080080061a61915340100001dec")));

        verifyFrame(
                binary("C0866300CD1400002273231400580008011308A2E68DA10110002006280030003800400048005000600068007000142B08EC979EB60410EEB7CC8C02180020002804300038A2E68DA1012C33080010001800200028003000344308381000180220382800300244DF2A"),
                decoder.decode(null, null, binary("C0866300CD1400002273231400580008011308A2E68DA10110002006280030003800400048005000600068007000142B08EC979EB60410EEB7CC8C02180020002804300038A2E68DA1012C33080010001800200028003000344308381000180220382800300244DF2A")));

    }

}
