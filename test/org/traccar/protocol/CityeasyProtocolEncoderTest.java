package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Command;

import javax.xml.bind.DatatypeConverter;

public class CityeasyProtocolEncoderTest {

    @Test
    public void testEncode() throws Exception {

        CityeasyProtocolEncoder encoder = new CityeasyProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, 6 * 3600);

        Assert.assertEquals(encoder.encodeCommand(command), ChannelBuffers.wrappedBuffer(
                DatatypeConverter.parseHexBinary("5353001100080001680000000B60820D0A")));

    }

}
