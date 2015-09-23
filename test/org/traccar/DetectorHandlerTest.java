package org.traccar;

import org.junit.Test;

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
