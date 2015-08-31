package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Command;

public class KhdProtocolEncoderTest {

    @Test
    public void testEncode() throws Exception {

        KhdProtocolEncoder encoder = new KhdProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals(encoder.encodeCommand(command), ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2929390006000000003F0D")));

    }

}
