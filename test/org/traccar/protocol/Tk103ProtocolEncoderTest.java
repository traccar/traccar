package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.Context;
import org.traccar.ProtocolTest;
import org.traccar.database.IdentityManager;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class Tk103ProtocolEncoderTest extends ProtocolTest {

    private static IdentityManager t580wManager = new IdentityManager() {

        private Device createDevice() {
            Device device = new Device();
            device.setId(1);
            device.setName("test");
            device.setUniqueId("123456789012345");
            device.set(Command.KEY_DEVICE_PASSWORD, "654321");
            return device;
        }

        @Override
        public Device getById(long id) {
            return createDevice();
        }

        @Override
        public Device getByUniqueId(String uniqueId) {
            return createDevice();
        }

        @Override
        public Position getLastPosition(long deviceId) {
            return null;
        }

        @Override
        public boolean isLatestPosition(Position position) {
            return true;
        }

        @Override
        public boolean lookupAttributeBoolean(
                long deviceId, String attributeName, boolean defaultValue, boolean lookupConfig) {
            return deviceId == 1 && attributeName == "tk103.deviceT580W" && lookupConfig ? true : defaultValue;
        }

        @Override
        public String lookupAttributeString(
                long deviceId, String attributeName, String defaultValue, boolean lookupConfig) {
            return defaultValue;
        }

        @Override
        public int lookupAttributeInteger(
                long deviceId, String attributeName, int defaultValue, boolean lookupConfig) {
            return defaultValue;
        }

        @Override
        public long lookupAttributeLong(
                long deviceId, String attributeName, long defaultValue, boolean lookupConfig) {
            return defaultValue;
        }
    };

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

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*getposl*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 60);

        Assert.assertEquals("(123456789012345AR00003C0000)", encoder.encodeCommand(command));

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*routetrack*99*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodePositionStop() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        Assert.assertEquals("(123456789012345AR0000000000)", encoder.encodeCommand(command));

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*routetrackoff*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeGetVersion() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        Assert.assertEquals("(123456789012345AP07)", encoder.encodeCommand(command));

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*about*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeRebootDevice() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();
        
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        Assert.assertEquals("(123456789012345AT00)", encoder.encodeCommand(command));

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,88888888,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

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
    public void testEncodeT580WIdentification() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_IDENTIFICATION);

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,999999,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeT580WSosOn() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, true);

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*soson*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeT580WSosOff() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ALARM_SOS);
        command.set(Command.KEY_ENABLE, false);

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*sosoff*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeT580WCustom() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "any text is ok");

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,any text is ok,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeT580WSetConnection() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_CONNECTION);
        command.set(Command.KEY_SERVER, "1.2.3.4");
        command.set(Command.KEY_PORT, "5555");

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*setip*1*2*3*4*5555*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

    @Test
    public void testEncodeT580WSosNumber() throws Exception {

        Tk103ProtocolEncoder encoder = new Tk103ProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, "0");
        command.set(Command.KEY_PHONE, "+55555555555");

        IdentityManager old = Context.getIdentityManager();
        Context.init(t580wManager);

        try {
            Assert.assertEquals("[begin]sms2,*master*654321*+55555555555*,[end]", encoder.encodeCommand(command));
        } finally {
            Context.init(old);
        }

    }

}
