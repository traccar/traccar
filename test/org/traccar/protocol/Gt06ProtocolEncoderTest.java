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
                "787812800c0000000052656c61792c312300009dee0d0a")));

    }

}
