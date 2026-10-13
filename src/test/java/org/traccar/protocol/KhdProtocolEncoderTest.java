package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.config.Config;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.session.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KhdProtocolEncoderTest extends ProtocolTest {

    private KhdProtocolEncoder encoderFor(String uniqueId) throws Exception {
        var encoder = new KhdProtocolEncoder(null);
        var device = mock(Device.class);
        when(device.getUniqueId()).thenReturn(uniqueId);
        var cacheManager = mock(CacheManager.class);
        when(cacheManager.getConfig()).thenReturn(mock(Config.class));
        when(cacheManager.getObject(eq(Device.class), anyLong())).thenReturn(device);
        encoder.setCacheManager(cacheManager);
        return encoder;
    }

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new KhdProtocolEncoder(null));
        var channel = channel(encoder);

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_ENGINE_STOP);
        verifyEncode(channel, command, binary("29293900065981972d5d0d"));

        command.setType(Command.TYPE_POSITION_SINGLE);
        verifyEncode(channel, command, binary("29293000065981972d540d"));

        // GS18 §2.2 periodic: interval 15s, default count 65535 (continuous)
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 15);
        verifyEncode(channel, command, binary("29297b000a5981972d000fffff1c0d"));

        // GS18 §2.2 periodic: interval 15s x 10 (matches PDF example payload)
        command.set("count", 10);
        verifyEncode(channel, command, binary("29297b000a5981972d000f000a160d"));

        // GS18 §2.4 reset restart
        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command, binary("29293200065981972d560d"));

        // GS18 §2.3 scheduling text
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "Hello!");
        verifyEncode(channel, command, binary("29293a000c5981972d48656c6c6f21370d"));

        // GS18 §2.3 param setting (Appendix D style, reply to D8 apply)
        command.set(Command.KEY_DATA, "<D:006>");
        verifyEncode(channel, command, binary("29293a000d5981972d3c443a3030363e1f0d"));

        // empty payload encodes to nothing
        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        assertNull(encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSimFakeIp() throws Exception {

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "<CKWW>");

        // SIM 13511223344 must address mask slot 0B9621AC, not legacy 0B96A12C.
        verifyEncode(channel(encoderFor("13511223344")), command,
                binary("29293a000c0b9621ac3c434b57573e2c0d"));

        // Another SIM slot: 14472923524 -> C8DCA318.
        verifyEncode(channel(encoderFor("14472923524")), command,
                binary("29293a000cc8dca3183c434b57573e930d"));

        // uint32 registration 3369902872 -> C8DCA318.
        verifyEncode(channel(encoderFor("3369902872")), command,
                binary("29293a000cc8dca3183c434b57573e930d"));

        command.setType(Command.TYPE_REBOOT_DEVICE);
        verifyEncode(channel, command,
                binary("29293200065981972d560d"));

        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "Hello!");
        verifyEncode(channel, command,
                binary("29293a000c5981972d48656c6c6f21370d"));

        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 15);
        verifyEncode(channel, command,
                binary("29297b000a5981972d000fffff1c0d"));

    }

}
