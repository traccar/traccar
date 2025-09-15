package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

public class IotmProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_DATA, "1");

        MqttPublishMessage encodedCommand = (MqttPublishMessage) encoder.encodeCommand(command);
        verifyFrame(binary("0202080079df0d8648700000040a00ffffff7f00000301b0b19e"), encodedCommand.payload());

    }

}
