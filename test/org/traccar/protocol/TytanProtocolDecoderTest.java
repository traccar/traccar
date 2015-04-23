package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;
import org.traccar.helper.TestDataManager;

public class TytanProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TytanProtocolDecoder decoder = new TytanProtocolDecoder(null);
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20E4FA7C77AFF3282C68D2F890800"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20e552e248007375bee8c02b3c002"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D5F0FF00001032552F9121D5325FCB8D11AFA0000205396504004C0602FB5B434118001765006603676B68006B80426C02E2C8206D2F9600"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D5C0FF00013D21552F9148D3325E8F8D11A80800060228816541061245FF674107C0001810130D21552F9148D3325E8F8D11A8080006022881654106121C46694107C0001810130D21552F9182D3325E8F8D11A8080006022881654106121C46694107C0001810140C"))));
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF221552e31f4d3325e908d11a7c8000602d60e68410612cd74694107c00018100f0b"))));
                
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF232552e0322d332603f8d1199a1100204bda204004c06024dea454118000e6515661b677068626b80486c02e2ae586d319600"))));
                
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20e552e259707375bee8c02b3c002"))));
                        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20e552e259707375bee8c02b3c002"))));

    }

}
