package org.traccar.protocol;

import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;
import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Jt600ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Jt600ProtocolDecoder decoder = new Jt600ProtocolDecoder(new Jt600Protocol());

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24311021600111001B16021105591022329862114046227B0598095080012327951435161F"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24312082002911001B171012052831243810120255336425001907190003FD2B91044D1FA0"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24312082002911001B1710120533052438099702553358450004061E0003EE000000000C00"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24608111888821001B09060908045322564025113242329F0598000001003F0000002D00AB"))));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "(3110312099,W01,11404.6204,E,2232.9961,N,A,040511,063736,4,7,100,4,17,1,1,company)", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "(3120820029,W01,02553.3555,E,2438.0997,S,A,171012,053339,0,8,20,6,31,5,20,20)", Charset.defaultCharset())));

        /*verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "(3330104377,U01,010100,010228,F,00.000000,N,000.000000,E,0,0,0,0%,00001000000000,741,14,22,0,206)", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "(6221107674,2,U09,129,2,A,280513113036,E,02711.0500,S,1721.0876,A,030613171243,E,02756.7618,S,2300.0325,3491,538200,14400,1)", Charset.defaultCharset())));*/

    }

}
