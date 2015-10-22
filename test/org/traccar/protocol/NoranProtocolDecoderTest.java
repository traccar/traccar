package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;

public class NoranProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        NoranProtocolDecoder decoder = new NoranProtocolDecoder(new NoranProtocol());
        
        //verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
        //        "28003200c38000d900fcc97a416b1a7a42b43eef3d4e523039473034383737000000000092fcda4a"));
        
        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "3400080001090000000000001D43A29BE842E62520424E523039423036363932000031322D30332D30352031313A34373A343300"));
        
        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "34000800010c000000000080a3438e20944149bd07c24e523039423139323832000031352d30342d32362030383a34333a353300"));

        verifyNothing(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "0f0000004e52303946303431353500"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "22000800010c008a007e9daa42317bdd41a7f3e2384e523039463034313535000000"));

        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "34000800010c0000000000001c4291251143388d17c24e523039423131303930000031342d31322d32352030303a33333a303700"));
        
        verifyPosition(decoder, binary(ByteOrder.LITTLE_ENDIAN,
                "34000800010c00000000000000006520944141bd07c24e523039423139323832000031352d30342d32352030303a30333a323200"));

    }

}
