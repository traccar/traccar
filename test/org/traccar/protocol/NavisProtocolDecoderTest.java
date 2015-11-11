package org.traccar.protocol;

import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;

import org.junit.Test;

public class NavisProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NavisProtocolDecoder decoder = new NavisProtocolDecoder(new NavisProtocol());

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "404E5443010000007B000000130044342A3E533A383631373835303035323035303739"));

        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "404e544301000000000000005a002e6c2a3e410125d7540100001512233a0b0a0f08026300000a000b000b00020000000000000000000c12233b0b0a0f03fd6d3f0fde603f00000000ba0051e0c845000000000000000000000000000000000000000000000080808080"));

        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "404E5443010000007B0000005A0050692A3E410125DB0E00000015110707110A0C0880630000AA39A2381600020000000000000000000C110708110A0CB389793F1AEF263F00000000120034F516440000000000000000000000FAFF000000FAFF000000FAFF80808080"));

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "404E5443010000007B000000130047372A3E533A383631373835303035313236303639"));

    }

}
