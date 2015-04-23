package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class AutoFonProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        AutoFonProtocolDecoder decoder = new AutoFonProtocolDecoder(null);

        assertNull(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "10556103592310314825728F"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "111E00000000000000000100007101010B0C020302010B0C0005A053FFFFFFFF02010B0C00276047FFFFFFFF1F5600FA000176F218C7850C0B0B0C203A033DBD46035783EF009E00320014FFFF45"))));

        //verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
        //        "12060000007501010B0C00089CFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000003E7FFFF02007601010B0C00269CFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000003E7FFFF4A007601010B0C01089CFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000003E7FFFF04007501010B0C01269CFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000003E7FFFF80007601010B0C02089CFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000003E7FFFFA6007501010B0C02231F5600FA000176F218C70000000000000000000000000000000000000003E7FFFF9629"))));

    }

}
