package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class PiligrimProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        PiligrimProtocolDecoder decoder = new PiligrimProtocolDecoder(new PiligrimProtocol());
        
        HttpRequest msg1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/bingps?imei=868204005544720&csq=18&vout=00&vin=4050&dataid=00000000");
        msg1.setContent(ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "fff2200d4110061a32354f3422310062000a0005173b0000a101000300005e00fff2200d4110100932354f2b22310042000b000e173b00009f01000700006000")));
        verify(decoder.decode(null, null, msg1));

    }

}
