package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.traccar.helper.TestDataManager;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class PiligrimProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        PiligrimProtocolDecoder decoder = new PiligrimProtocolDecoder(new TestDataManager(), null, null);
        
        HttpRequest msg1 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/bingps?imei=868204005544720&csq=18&vout=00&vin=4050&dataid=00000000");
        int[] buf1 = {0xff,0xf2,0x20,0x0d,0x41,0x10,0x06,0x1a,0x32,0x35,0x4f,0x34,0x22,0x31,0x00,0x62,0x00,0x0a,0x00,0x05,0x17,0x3b,0x00,0x00,0xa1,0x01,0x00,0x03,0x00,0x00,0x5e,0x00,0xff,0xf2,0x20,0x0d,0x41,0x10,0x10,0x09,0x32,0x35,0x4f,0x2b,0x22,0x31,0x00,0x42,0x00,0x0b,0x00,0x0e,0x17,0x3b,0x00,0x00,0x9f,0x01,0x00,0x07,0x00,0x00,0x60,0x00};
        msg1.setContent(ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertArray(buf1)));
        verify(decoder.decode(null, null, msg1));

    }

}
