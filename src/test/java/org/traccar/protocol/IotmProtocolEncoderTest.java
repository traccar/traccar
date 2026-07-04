package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    private IotmProtocolEncoder newEncoder(boolean permanentOutputControl) throws Exception {
        Config config = new Config();
        config.setString(Keys.IOTM_PERMANENT_OUTPUT_CONTROL, Boolean.toString(permanentOutputControl));
        return inject(new IotmProtocolEncoder(null, config));
    }

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null, new Config()));

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
    public void testEncodePermanentOutputControl() throws Exception {

        var encoder = newEncoder(true);

        Command command = outputCommand(1, 1);

        MqttPublishMessage encodedCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0801010142"), encodedCommand.payload());

    }

}
