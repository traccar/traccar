package org.traccar.protocol;

import org.traccar.ProtocolTest;

import org.junit.Test;

public class NavisFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        NavisProtocolDecoder protocolDecoder = new NavisProtocolDecoder(null);
        NavisFrameDecoder frameDecoder = new NavisFrameDecoder(protocolDecoder);

        // Handshake
        verifyNull(protocolDecoder, binary(
                "404E5443010000007B000000130044342A3E533A383631373835303035323035303739"));

        // NTCB F5.2 - Single
        verifyFrame(binary(
                "404e5443010000000000000059009adb2a3e54250000000000ff1500040b0a1008291838001200760ee600000000000000000000000f1500040b0a10ac20703fb1aec23f00000000320149668f430000000000000000000000000000000000000000000000f3808080"),
                frameDecoder.decode(null, null, binary("404e5443010000000000000059009adb2a3e54250000000000ff1500040b0a1008291838001200760ee600000000000000000000000f1500040b0a10ac20703fb1aec23f00000000320149668f430000000000000000000000000000000000000000000000f3808080")));

        // FLEX 1.0 - Negotiation
        verifyNull(protocolDecoder, binary(
                "404e544301000000aaecf6021300c8712a3e464c4558b00a0a45ffff300a08080f8388"));

        // FLEX 1.0 - Single
        verifyFrame(binary(
                "7e54040000000400000030129957405c000b00632f9857405ccace03021e129101a103000000000000c4005ba3fe3b00000000120046100000000000001aff7f000080bfffff80000080bfffffffff9f"),
                frameDecoder.decode(null, null, binary("7e54040000000400000030129957405c000b00632f9857405ccace03021e129101a103000000000000c4005ba3fe3b00000000120046100000000000001aff7f000080bfffff80000080bfffffffff9f")));

        // FLEX 1.0 - Array (1 message)
        verifyFrame(binary(
                "7e4101080000000917c057405c002b001833c057405cbbce030225129101a00300007c6102408900400c1b3cfce3b23a12004710e000000000001bff7f000080bfffff80000080bfffffffffb2"),
                frameDecoder.decode(null, null, binary("7e4101080000000917c057405c002b001833c057405cbbce030225129101a00300007c6102408900400c1b3cfce3b23a12004710e000000000001bff7f000080bfffff80000080bfffffffffb2")));
    }

}
