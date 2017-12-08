package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.TestIdentityManager;
import org.traccar.model.Command;
import org.traccar.model.Device;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeEngineStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        Assert.assertEquals("(123456789012345AV010)", encoder.encodeCommand(command));

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
    public void testEncodePositionSingleAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        Assert.assertEquals("[begin]sms2,*getposl*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionPeriodicAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);

        Assert.assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionStopAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        Assert.assertEquals("[begin]sms2,*routetrackoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeGetVersionAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        Assert.assertEquals("[begin]sms2,*about*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeRebootDeviceAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        Assert.assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeIdentificationAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_IDENTIFICATION);

        Assert.assertEquals("[begin]sms2,999999,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOnAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);

        Assert.assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOffAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, false);

        Assert.assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustomAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "any text is ok");

        Assert.assertEquals("[begin]sms2,any text is ok,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSetConnectionAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, "5555");

        Assert.assertEquals("[begin]sms2,*setip*1*2*3*4*5555*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosNumberAlternative() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, "0");
        command.set(Command.KEY_PHONE, "+55555555555");
        command.set(Command.KEY_DEVICE_PASSWORD, "232323");

        Assert.assertEquals("[begin]sms2,*master*232323*+55555555555*,[end]", encoder.encodeCommand(command));

    }

}
