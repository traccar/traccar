package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class TytanProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TytanProtocolDecoder decoder = new TytanProtocolDecoder(null);
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "d5300000689d65556877fbd437a09d8ea89360005a23124e410201213704000907000009ffffffffffffffff0affffffffffffffff18ff63ff7f7fff65ff66ff67ff68ff69ff6b00ff6cffffffff6dff7fffffffff81ffffffff82ffff83ffffffffffffffff88ffff9600"))));
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "d7700000689d205544713bd3378f2c8e9fe181005affdf9041020000000602a1b7563b0700002055447159d3378f2d8e9fe281005affdf9041020000000602a1b7563b0700002055447177d3378f2e8e9fe181005a861a9141020000000602a1b7563b0700002055447195f3378f2d8e9fe281005a1ce78d41020000000602a1b7563b07000020554471b3d3378f2b8e9fe281005a58d88b41020000000602a1b7563b07000020554471d1f3378f2b8e9fe281005a58d88b41020000000602a1b7563b07000020554471efd3378f2a8e9fe381005a94ac8d41020000000602a1b7563b070000205544720df3378f2c8e9fe281005a1ce78d41020000000602a1b7563b070000205544722bd3378f2d8e9fe281005a48638b41020000000602a1b7563b0700002055447249f3378f308e9fe181005adf128c41020000000602a1b7563b070000"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D5F0FF00001032552F9121D5325FCB8D11AFA0000205396504004C0602FB5B434118001765006603676B68006B80426C02E2C8206D2F9600"))));
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20E4FA7C77AFF3282C68D2F890800"))));

        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "D56000000AF20e552e248007375bee8c02b3c002"))));

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
        
        verify(decoder.decode(null, null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "d7680000689d2055447267d3378f308e9fe181005ad1808f41020000000602a1b7563b0700002055447285f3378f308e9fe281005aee878c41020000000602a1b7563b07000020554472a3f3378f2e8e9fe381005ab3968e41020000000602a1b7563b07000020554472c1f3378f308e9fe281005ab3968e41020000000602a1b7563b07000020554472dff3378f2e8e9fe481005adf128c41020000000602a1b7563b07000020554472fdd3378f2e8e9fe481005a84548941020000000602a1b7563b070000205544731bd3378f2e8e9fe481005a49468f41020000000602a1b7563b0700002055447339f3378f2e8e9fe481005a3ad18e41020000000602a1b7563b0700002055447358f3378f308e9fe381005ab3968e41020000000602b909a13b0700002055447376f3378f308e9fe481005ac20b8f41020000000602b909a13b070000"))));

    }

}
