package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.junit.jupiter.api.Test;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.NetworkMessage;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Position;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OmniProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new OmniProtocolDecoder(null));

        verifyPosition(decoder, text(
                "*SCOR,OM,123456789012345,D0,0,120001.0,A,2457.8101,N,12125.5393,E,"
                        + "8,0.8,010124,10.0,M,A#"));

    }

    @Test
    public void testFrameWithoutLineBreak() throws Exception {

        var channel = new EmbeddedChannel(
                new CharacterDelimiterFrameDecoder(4 * 1024, false, "#", "\n"),
                new StringDecoder());

        assertNotNull(channel.writeInbound(buffer(
                "*SCOR,OM,123456789012345,D0,0,120001.0,A,2457.8101,N,12125.5393,E,"
                        + "8,0.8,010124,10.0,M,A#")));
        verifyPosition(inject(new OmniProtocolDecoder(null)), channel.readInbound());

    }

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);

        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,D1,300#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodePositionStop() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_STOP);

        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,D1,0#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeIdentification() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_IDENTIFICATION);

        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,I0#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeGetVersion() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_VERSION);

        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,G0#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeNamedCustomCommands() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);

        command.set(Command.KEY_DATA, "MODE_ECO");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S7,0,1,0,0#\n")),
                encoder.encodeCommand(null, command));

        command.set(Command.KEY_DATA, "UNLOCK_HELMET_LOCK");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,L5,2#\n")),
                encoder.encodeCommand(null, command));

        command.set(Command.KEY_DATA, "S7,0,3,0,0");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S7,0,3,0,0#\n")),
                encoder.encodeCommand(null, command));

        command.set(Command.KEY_DATA, "REQUEST_BLE_KEY");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,K0#\n")),
                encoder.encodeCommand(null, command));

        command.set(Command.KEY_DATA, "LIGHT_STRIP_ON");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S2,1#\n")),
                encoder.encodeCommand(null, command));

        command.set(Command.KEY_DATA, "GET_RFID");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,C0#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testDecodeWarningAlarm() throws Exception {

        var decoder = inject(new OmniProtocolDecoder(null));

        Position position = (Position) decoder.decode(
                null, null, "*SCOR,OM,123456789012345,W0,4#\n");

        assertNotNull(position);
        assertTrue(position.getFixTime().after(new java.util.Date(915148800000L)));
        assertTrue(position.getAttributes().containsKey("omni"));
        assertTrue(position.getAttributes().containsValue(Position.ALARM_LOW_BATTERY));

    }

    @Test
    public void testDecodeAllDocumentResultCommands() throws Exception {

        var decoder = inject(new OmniProtocolDecoder(null));

        for (String type : new String[] {
                "D1", "S2", "S4", "S5", "S7", "V0", "V1", "G0", "K0", "I0", "M0",
                "L5", "Z0", "Z1", "D2", "U0", "U1", "U2", "U5", "U6", "C0", "B0"}) {
            verifyAttribute(
                    decoder, "*SCOR,OM,123456789012345," + type + ",1,2,3#\n",
                    Position.KEY_RESULT, type + ",1,2,3");
        }

    }

    @Test
    public void testEncodeSpeedLimit() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_SPEED_LIMIT);
        command.set("speedLimit", "12");

        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S4,0,0,0,0,0,12,12,12#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeStandardDocumentCommands() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);

        command.setType(Command.TYPE_CONFIGURATION);
        command.set(Command.KEY_DATA, "0,1,0,0");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S5,0,1,0,0#\n")),
                encoder.encodeCommand(null, command));

        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_DATA, "2");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,V0,2#\n")),
                encoder.encodeCommand(null, command));

        command.setType(Command.TYPE_SET_INDICATOR);
        command.set(Command.KEY_DATA, "1");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,S2,1#\n")),
                encoder.encodeCommand(null, command));

        command.setType(Command.TYPE_FIRMWARE_UPDATE);
        command.set(Command.KEY_DATA, "http://example.com/iot.bin");
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*SCOS,OM,123456789012345,U0,http://example.com/iot.bin#\n")),
                encoder.encodeCommand(null, command));

    }

    @Test
    public void testEncodeUnlockRequest() throws Exception {

        var encoder = inject(new OmniProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        ByteBuf encoded = (ByteBuf) encoder.encodeCommand(null, command);
        assertTrue(encoded.readUnsignedShort() == 0xffff);
        assertTrue(encoded.toString(java.nio.charset.StandardCharsets.US_ASCII)
                .matches("\\*SCOS,OM,123456789012345,R0,0,20,1234,\\d+#\\n"));

    }

    @Test
    public void testAcknowledgeCmdrLockMessage() throws Exception {

        var channel = new EmbeddedChannel(inject(new OmniProtocolDecoder(null)));

        channel.writeInbound(new NetworkMessage(
                "*CMDR,OM,123456789012345,000000000000,L1,0,0,0#\n", null));

        NetworkMessage response = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof NetworkMessage message && message.getMessage() instanceof ByteBuf) {
                response = message;
            }
        }

        assertNotNull(response);
        assertInstanceOf(ByteBuf.class, response.getMessage());
        verifyFrame(
                concatenateBuffers(binary("ffff"), buffer("*CMDS,OM,123456789012345,000000000000,L1#\n")),
                response.getMessage());

    }

    @Test
    public void testEncodeCommandForCmdrLockDialect() throws Exception {

        var decoder = inject(new OmniProtocolDecoder(null));
        var channel = new EmbeddedChannel(decoder);
        var encoder = inject(new OmniProtocolEncoder(null));

        channel.writeInbound(new NetworkMessage(
                "*CMDR,OM,123456789012345,000000000000,L1,0,0,0#\n", null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);

        ByteBuf encoded = (ByteBuf) encoder.encodeCommand(channel, command);
        assertTrue(encoded.readUnsignedShort() == 0xffff);
        assertTrue(encoded.toString(java.nio.charset.StandardCharsets.US_ASCII)
                .matches("\\*CMDS,OM,123456789012345,\\d{12},R0,0,20,1234,\\d+#\\n"));

    }

}
