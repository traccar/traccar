package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import static org.traccar.helper.DecoderVerifier.verify;

public class TytanProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        TytanProtocolDecoder decoder = new TytanProtocolDecoder(new TytanProtocol());

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "B5005690000068494F561CEAE932325FD28D11A299000702000063045532030066013567018768014B6901286B0240396C04030785986D013E7F040000A7CE81040000A76C82027EAB83080FA01068FFFF0F3C880202583156"))));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "b50069a00000689d315604512b32378f1a8e9fe094005a04d7c84b41020300ab250402140c0702c0006501006601006b0280646c0402883db0315604525732378f1d8e9fdd94005a04d7c84b41020300ab250402140c0702c0006501006601006b0280646c0402883db08887"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "b50028080000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c070200000901"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "b500280a0000689d215602772f00378f1b8e9fdd98005a042efb3e4102030000000402140c07020000da20"))));

    }

}
