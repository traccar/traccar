package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;
import org.traccar.api.resource.CommandResource;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class Jt808VideoFileDownloadTest extends ProtocolTest {

    private Command createDownloadCommand() {
        var command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VIDEO_FILE_DOWNLOAD);
        command.set(Command.KEY_SERVER, "192.0.2.10");
        command.set(Command.KEY_PORT, 21);
        command.set("username", "prueba");
        command.set("password", "temporal");
        command.set("path", "/canal1/");
        command.set(Command.KEY_INDEX, 1);
        command.set("from", "2026-11-02T22:00:00Z");
        command.set("to", "2026-11-02T22:00:30Z");
        command.set("alarmMask", "8000000000000001");
        command.set("resourceType", 3);
        command.set("streamType", 1);
        command.set("storageType", 0);
        command.set("networkConditions", 4);
        return command;
    }

    private Jt808ProtocolEncoder prepareEncoder(String timezone) throws Exception {
        var encoder = inject(new Jt808ProtocolEncoder(null));
        encoder.setModelOverride("JC371");
        when(encoder.getCacheManager().getObject(Device.class, 1).getAttributes())
                .thenReturn(Map.of("decoder.timezone", timezone));
        return encoder;
    }

    @Test
    public void testCommandTypeAvailable() throws Exception {
        assertTrue(new CommandResource().get(0, false).stream()
                .anyMatch(type -> type.type().equals(Command.TYPE_VIDEO_FILE_DOWNLOAD)));
    }

    @Test
    public void testEncodeDownloadWithDeviceTimezoneAndUnsignedAlarmMask() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = prepareEncoder("GMT-03:00");
        var channel = channel(decoder, encoder);
        verifyEncode(channel, createDownloadCommand(), binary(
                "7e9206003f0b3a73ce2ff200000a3139322e302e322e31300015067072756562610874656d706f72616c082f63616e616c312f012611021900002611021900308000000000000001030100041f7e"));
    }

    @Test
    public void testEncodeDownloadWithVersionedHeader() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setProtocolVersion(3);
        var encoder = prepareEncoder("GMT-03:00");
        var channel = channel(decoder, encoder);
        verifyEncode(channel, createDownloadCommand(), binary(
                "7e9206403f030000012345678901234500000a3139322e302e322e31300015067072756562610874656d706f72616c082f63616e616c312f01261102190000261102190030800000000000000103010004e37e"));
    }

    @ParameterizedTest
    @CsvSource({
            "index, 0", "index, 32", "port, 0", "port, 65536",
            "resourceType, 4", "streamType, 3", "storageType, 3",
            "networkConditions, 0", "networkConditions, 8",
            "alarmMask, 10000000000000000", "alarmMask, zz",
            "from, 2026-11-02T22:00:00", "to, 2026-11-02T21:59:59Z",
            "to, 2026-11-02T22:00:00Z", "from, 1999-11-02T22:00:00Z",
            "to, 2100-11-02T22:00:30Z", "from, 2026-11-02T22:00:00.500Z"
    })
    public void testRejectInvalidFields(String key, String value) throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = prepareEncoder("GMT-03:00");
        var channel = channel(decoder, encoder);
        var command = createDownloadCommand();
        command.set(key, value);
        assertThrows(RuntimeException.class, () -> encoder.encodeCommand(channel, command));
        assertTrue(channel.outboundMessages().isEmpty());
    }

    @Test
    public void testRejectOverflowAndFractionalValues() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = prepareEncoder("GMT-03:00");
        var channel = channel(decoder, encoder);
        var command = createDownloadCommand();
        command.set(Command.KEY_INDEX, 4294967297L);
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
        command.set(Command.KEY_INDEX, 1.5);
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
    }

    @Test
    public void testRejectInvalidFtpStrings() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = prepareEncoder("GMT-03:00");
        var channel = channel(decoder, encoder);
        for (String value : new String[]{"x".repeat(256), "invalido\r\n", "invalido\0", "a\u00f1o"}) {
            var command = createDownloadCommand();
            command.set("password", value);
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
        }
        var command = createDownloadCommand();
        for (String key : new String[]{"server", "username", "password", "path"}) {
            command.set(key, "x".repeat(255));
        }
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
    }

    @Test
    public void testRejectAmbiguousDeviceTime() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = prepareEncoder("America/New_York");
        var channel = channel(decoder, encoder);
        var command = createDownloadCommand();
        command.set("from", "2026-11-01T05:15:00Z");
        command.set("to", "2026-11-01T05:16:00Z");
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
    }

    @Test
    public void testDecodeUploadCompletion() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        verifyDecode(decoder, binary("7e120600030b3a73ce2ff20007123400677e"),
                position(Checks.ATTRIBUTES).outdated(true).attribute(Position.KEY_RESULT,
                        "videoFileDownload: serial=4660, stage=completion, result=0"));
        verifyDecode(decoder, binary("7e120600030b3a73ce2ff20007123401667e"),
                position(Checks.ATTRIBUTES).outdated(true).attribute(Position.KEY_RESULT,
                        "videoFileDownload: serial=4660, stage=completion, result=1"));
    }

    @Test
    public void testDecodeUploadAcknowledgment() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        verifyDecode(decoder, binary("7e000100050b3a73ce2ff200071234920600e07e"));
        verifyDecode(decoder, binary("7e000100050b3a73ce2ff200071234920601e17e"),
                position(Checks.ATTRIBUTES).outdated(true).attribute(Position.KEY_RESULT,
                        "videoFileDownload: serial=4660, stage=rejected, result=1"));
        verifyDecode(decoder, binary("7e000100050b3a73ce2ff200071234910101e57e"));
    }

    @Test
    public void testIgnoreInvalidCompletionLength() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        verifyDecode(decoder, binary("7e120600020b3a73ce2ff200071234667e"));
        verifyDecode(decoder, binary("7e120600040b3a73ce2ff2000712340000607e"));
    }

    @Test
    public void testAcknowledgeUploadCompletion() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var channel = channel(decoder);
        assertInstanceOf(Position.class,
                decoder.decode(channel, null, binary("7e120600030b3a73ce2ff20007123400677e")));
        var message = assertInstanceOf(NetworkMessage.class, channel.readOutbound());
        var data = assertInstanceOf(ByteBuf.class, message.getMessage());
        try {
            assertEquals("7e800100050b3a73ce2ff200000007120600c67e", ByteBufUtil.hexDump(data));
        } finally {
            data.release();
        }
    }

}
