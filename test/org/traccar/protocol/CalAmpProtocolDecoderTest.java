package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class CalAmpProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        CalAmpProtocolDecoder decoder = new CalAmpProtocolDecoder(new TestDataManager(), null, null);
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "830545420185450101010200075517fb335516c5c40fb1aea4cf4cbf250000000000000000008900260015ffb10f001108110a0000")), null));
        
        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "830543321494750101010A00085492798A0EC4F9E71BDA3B81005600040F1F33050000030000000076000000000000000000000000")), null));

    }

}
