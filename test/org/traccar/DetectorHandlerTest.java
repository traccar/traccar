package org.traccar;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.traccar.helper.TestDataManager;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.protocol.Gps103ProtocolDecoder;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DetectorHandlerTest {

    @Test
    public void testCheckPipeline() throws Exception {

        /*ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "\r\n", "\n", ";"));
        pipeline.addLast("stringDecoder", new StringDecoder());
        pipeline.addLast("stringEncoder", new StringEncoder());
        pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(null));

        DetectorHandler.checkPipeline("gps103", pipeline, ChannelBuffers.copiedBuffer(
                "imei:869039001186913,tracker,1308282156,0,F,215630.000,A,5602.11015,N,9246.30767,E,1.4,,175.9,;", Charset.defaultCharset()));*/
    }

}
