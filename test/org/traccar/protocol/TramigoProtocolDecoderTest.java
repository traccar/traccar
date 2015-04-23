package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import java.nio.ByteOrder;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class TramigoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TramigoProtocolDecoder decoder = new TramigoProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "8000011bb0009e0001015b93032ef6f35994a9545472616d69676f3a204d6f76696e672c20302e3930206b6d205345206f66204372616e6562726f6f6b20466972652053746174696f6e2c2050656e726974682c205379646e65792c2041552c202d33332e37303732322c203135302e37313735392c2053452077697468207370656564203337206b6d2f682c2031393a3438204a616e20342020454f46"))));

        // Tramigo: Parked, 0.12 km E of McDonald's H.V. dela Costa, Makati, 11:07 Mar 27
        // Tramigo: Moving, 0.90 km SE of Cranebrook Fire Station, Penrith, Sydney, AU, -33.70722, 150.71759, SE with speed 37 km/h, 19:48 Jan 4  EOF

        //verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(
        //        new int[] {0x68,0x68,0x25,0x00,0x00,0x01,0x23,0x45,0x67,0x89,0x01,0x23,0x45,0x00,0x01,0x10,0x01,0x01,0x01,0x01,0x01,0x01,0x02,0x6B,0x3F,0x3E,0x02,0x6B,0x3F,0x3E,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x0D,0x0A}))));

    }

}
