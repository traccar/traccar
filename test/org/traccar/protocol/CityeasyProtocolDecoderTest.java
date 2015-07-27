package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class CityeasyProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CityeasyProtocolDecoder decoder = new CityeasyProtocolDecoder(new CityeasyProtocol());

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "545400153520000000000100010000000111000D0A"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "5454006135200000000001000332303134313131303039353430392C412C342C4E2C32322E3533373232382C452C3131342E3032323737342C302E312C312E392C35302E363B3436302C302C31303137332C343635322C34310000000B63130D0A"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "5454006135200000000001000432303134313131303039353330362C412C352C4E2C32322E3533373233352C452C3131342E3032323838312C302E322C312E362C35342E313B3436302C302C31303137332C343635322C343100000045EC620D0A"))));

    }

}
