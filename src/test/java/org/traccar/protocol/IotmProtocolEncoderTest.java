package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IotmProtocolEncoderTest extends ProtocolTest {

    private static final int OUTPUT_COMMAND_UNIQUE_ID_INDEX = 20;

    private Command outputCommand(int index, int data) {
        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, index);
        command.set(Command.KEY_DATA, data);
        return command;
    }

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null));

        Command command = outputCommand(1, 1);

        MqttPublishMessage encodedCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040a00ffffff7f00010301b0b19f"), encodedCommand.payload());
        assertEquals(1, encodedCommand.variableHeader().packetId());
        assertEquals(1, encodedCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX));

        MqttPublishMessage nextCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        assertEquals(2, nextCommand.variableHeader().packetId());
        assertEquals(2, nextCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX));
        assertNotEquals(
                encodedCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX),
                nextCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX));

    }

    @Test
    public void testEncodePermanentOutput() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null));

        Command command = outputCommand(1, 1);
        command.set("permanent", true);

        MqttPublishMessage encodedCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0801010142"), encodedCommand.payload());

        command.set(Command.KEY_DATA, 0);

        MqttPublishMessage nextCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0802010042"), nextCommand.payload());
        assertNotEquals(
                encodedCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX),
                nextCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX));

    }

    @Test
    public void testEncodePermanentOutputIndex() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null));

        Command command = outputCommand(2, 1);
        command.set("permanent", true);

        MqttPublishMessage encodedCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0901010143"), encodedCommand.payload());

        command.set(Command.KEY_DATA, 0);

        MqttPublishMessage nextCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0902010043"), nextCommand.payload());
        assertNotEquals(
                encodedCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX),
                nextCommand.payload().getUnsignedByte(OUTPUT_COMMAND_UNIQUE_ID_INDEX));

    }

    @Test
    public void testEncodePermanentOutputValidation() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null));

        Command indexCommand = outputCommand(3, 1);
        indexCommand.set("permanent", true);
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(indexCommand));

        Command dataCommand = outputCommand(1, 2);
        dataCommand.set("permanent", true);
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(dataCommand));

    }

}
