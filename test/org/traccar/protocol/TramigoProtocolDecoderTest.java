package org.traccar.protocol;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class TramigoProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TramigoProtocolDecoder decoder = new TramigoProtocolDecoder(new TramigoProtocol());
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "80005408b000af000101b23903677f00c8436d3842616c697365204f6e653a20416c6c756d616765206d61726368652064e974656374e92c20676172e92c20302e3735206b6d20452064652045636f6c65204175746f726f757465206465204b696e73686173612c2056696c6c65206465204b696e73686173612c204b696e73686173612c2043442c202d342e33343130362c2031352e33343931352c2030313a3030204a616e2031202020454f46"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, ChannelBufferTools.convertHexString(
                "8000011bb0009e0001015b93032ef6f35994a9545472616d69676f3a204d6f76696e672c20302e3930206b6d205345206f66204372616e6562726f6f6b20466972652053746174696f6e2c2050656e726974682c205379646e65792c2041552c202d33332e37303732322c203135302e37313735392c2053452077697468207370656564203337206b6d2f682c2031393a3438204a616e20342020454f46"))));

        // Tramigo: Parked, 0.12 km E of McDonald's H.V. dela Costa, Makati, 11:07 Mar 27
        // Tramigo: Moving, 0.90 km SE of Cranebrook Fire Station, Penrith, Sydney, AU, -33.70722, 150.71759, SE with speed 37 km/h, 19:48 Jan 4  EOF

        //verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(
        //        new int[] {0x68,0x68,0x25,0x00,0x00,0x01,0x23,0x45,0x67,0x89,0x01,0x23,0x45,0x00,0x01,0x10,0x01,0x01,0x01,0x01,0x01,0x01,0x02,0x6B,0x3F,0x3E,0x02,0x6B,0x3F,0x3E,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x0D,0x0A}))));

    }

}
