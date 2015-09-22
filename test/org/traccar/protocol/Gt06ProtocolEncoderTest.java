package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Command;

public class Gt06ProtocolEncoderTest {

    @Test
    public void testEncode() throws Exception {

        Gt06ProtocolEncoder encoder = new Gt06ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals(encoder.encodeCommand(command), ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "78780e800800000000445944230001d09e0d0a")));

    }

}
