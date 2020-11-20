package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeOutputControl() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_DATA, "1");

        assertEquals("(123456789012345AV001)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeEngineStop() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        assertEquals("(123456789012345AV010)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionSingle() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        assertEquals("(123456789012345AP00)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionPeriodic() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);

        assertEquals("(123456789012345AR00003C0000)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionStop() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        assertEquals("(123456789012345AR0000000000)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeGetVersion() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        assertEquals("(123456789012345AP07)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeRebootDevice() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        assertEquals("(123456789012345AT00)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSetOdometer() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_ODOMETER);

        assertEquals("(123456789012345AX01)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionSingleAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        assertEquals("[begin]sms2,*getposl*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionPeriodicAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);

        assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodePositionStopAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        assertEquals("[begin]sms2,*routetrackoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeGetVersionAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        assertEquals("[begin]sms2,*about*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeRebootDeviceAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeIdentificationAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_IDENTIFICATION);

        assertEquals("[begin]sms2,999999,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOnAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);

        assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosOffAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, false);

        assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "AA00");

        assertEquals("(123456789012345AA00)", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustomAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "any text is ok");

        assertEquals("[begin]sms2,any text is ok,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSetConnectionAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, "5555");

        assertEquals("[begin]sms2,*setip*1*2*3*4*5555*,[end]", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSosNumberAlternative() {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder(null, true);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, "0");
        command.set(Command.KEY_PHONE, "+55555555555");
        command.set(Command.KEY_DEVICE_PASSWORD, "232323");

        assertEquals("[begin]sms2,*master*232323*+55555555555*,[end]", encoder.encodeCommand(command));

    }

}
