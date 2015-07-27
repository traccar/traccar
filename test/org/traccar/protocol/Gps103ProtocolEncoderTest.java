package org.traccar.protocol;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Command;

public class Gps103ProtocolEncoderTest {

    @Test
    public void testDecode() throws Exception {

        Gps103ProtocolEncoder encoder = new Gps103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_FIX);
        
        Map<String, Object> other = new HashMap<>();
        other.put(Command.KEY_FREQUENCY, 300l);
        
        command.setOther(other);
        
        Assert.assertEquals("**,123456789012345,C,05m;", encoder.encodeCommand(command));

    }

}
