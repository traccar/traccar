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

        command.setDeviceId(2);

        Assert.assertEquals("[begin]sms2,*getposl*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);

        Assert.assertEquals("(123456789012345AR00003C0000)", encoder.encodeCommand(command));

        command.setDeviceId(2);

        Assert.assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        Assert.assertEquals("(123456789012345AR0000000000)", encoder.encodeCommand(command));

        command.setDeviceId(2);

        Assert.assertEquals("[begin]sms2,*routetrackoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeGetVersion() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        Assert.assertEquals("(123456789012345AP07)", encoder.encodeCommand(command));

        command.setDeviceId(2);

        Assert.assertEquals("[begin]sms2,*about*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeRebootDevice() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        Assert.assertEquals("(123456789012345AT00)", encoder.encodeCommand(command));

        command.setDeviceId(2);

        Assert.assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));

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
    public void testEncodeIdentification() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_IDENTIFICATION);

        Assert.assertEquals("[begin]sms2,999999,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOn() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);

        Assert.assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOff() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, false);

        Assert.assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "any text is ok");

        Assert.assertEquals("[begin]sms2,any text is ok,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSetConnection() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, "5555");

        Assert.assertEquals("[begin]sms2,*setip*1*2*3*4*5555*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosNumber() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(2);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, "0");
        command.set(Command.KEY_PHONE, "+55555555555");

        Assert.assertEquals("[begin]sms2,*master*123456*+55555555555*,[end]", encoder.encodeCommand(command));

    }

}
