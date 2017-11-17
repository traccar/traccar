package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals("(123456789012345AV011)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionSingle() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        Assert.assertEquals("(123456789012345AP00)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);

        Assert.assertEquals("(123456789012345AR00003C0000)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        Assert.assertEquals("(123456789012345AR0000000000)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeGetVersion() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        Assert.assertEquals("(123456789012345AP07)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeRebootDevice() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        Assert.assertEquals("(123456789012345AT00)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSetOdometer() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_ODOMETER);

        Assert.assertEquals("(123456789012345AX01)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeT580WRebootDevice() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType("T580W_rebootDevice");

        Assert.assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeT580WSosOn() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType("T580W_alarmSosOn");

        Assert.assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeT580WSosOff() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType("T580W_alarmSosOff");

        Assert.assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeT580WRouteTrack() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType("T580W_positionRealtime");

        Assert.assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));

    }

}
