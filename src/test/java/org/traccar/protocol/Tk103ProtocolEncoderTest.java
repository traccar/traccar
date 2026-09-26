package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeOutputControl() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_DATA, "1");

        verify(channel, command,
                text("(123456789012345AV001)"));

    }

    @Test
    public void testEncodeEngineStop() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verify(channel, command,
                text("(123456789012345AV010)"));

    }

    @Test
    public void testEncodePositionSingle() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        verify(channel, command,
                text("(123456789012345AP00)"));

    }

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);

        verify(channel, command,
                text("(123456789012345AR00003C0000)"));

    }

    @Test
    public void testEncodePositionStop() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        verify(channel, command,
                text("(123456789012345AR0000000000)"));

    }

    @Test
    public void testEncodeGetVersion() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        verify(channel, command,
                text("(123456789012345AP07)"));

    }

    @Test
    public void testEncodeRebootDevice() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verify(channel, command,
                text("(123456789012345AT00)"));

    }

    @Test
    public void testEncodeSetOdometer() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_ODOMETER);

        verify(channel, command,
                text("(123456789012345AX01)"));

    }

    @Test
    public void testEncodePositionSingleAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        verify(channel, command,
                text("[begin]sms2,*getposl*,[end]"));

    }

    @Test
    public void testEncodePositionPeriodicAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);

        verify(channel, command,
                text("[begin]sms2,*routetrack*99*,[end]"));

    }

    @Test
    public void testEncodePositionStopAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        verify(channel, command,
                text("[begin]sms2,*routetrackoff*,[end]"));

    }

    @Test
    public void testEncodeGetVersionAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        verify(channel, command,
                text("[begin]sms2,*about*,[end]"));

    }

    @Test
    public void testEncodeRebootDeviceAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        verify(channel, command,
                text("[begin]sms2,88888888,[end]"));

    }

    @Test
    public void testEncodeIdentificationAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_IDENTIFICATION);

        verify(channel, command,
                text("[begin]sms2,999999,[end]"));

    }

    @Test
    public void testEncodeSosOnAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);

        verify(channel, command,
                text("[begin]sms2,*soson*,[end]"));

    }

    @Test
    public void testEncodeSosOffAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, false);

        verify(channel, command,
                text("[begin]sms2,*sosoff*,[end]"));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "AA00");

        verify(channel, command,
                text("(123456789012345AA00)"));

    }

    @Test
    public void testEncodeCustomAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "any text is ok");

        verify(channel, command,
                text("[begin]sms2,any text is ok,[end]"));

    }

    @Test
    public void testEncodeSetConnectionAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, "5555");

        verify(channel, command,
                text("[begin]sms2,*setip*1*2*3*4*5555*,[end]"));

    }

    @Test
    public void testEncodeSosNumberAlternative() throws Exception {

        var channel = channel(inject(new Tk103ProtocolEncoder(null, true)));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 0);
        command.set(Command.KEY_PHONE, "+55555555555");
        command.set(Command.KEY_DEVICE_PASSWORD, "232323");

        verify(channel, command,
                text("[begin]sms2,*master*232323*+55555555555*,[end]"));

    }

}
