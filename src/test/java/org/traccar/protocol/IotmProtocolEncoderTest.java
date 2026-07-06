package org.traccar.protocol;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;

public class IotmProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new IotmProtocolEncoder(null, new Config()));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_DATA, 1);

        verifyFrame(binary("0202080079df0d8648700000040a00ffffff7f00010301b0b19f"),
                ((MqttPublishMessage) encoder.encodeCommand(command)).payload());

        Config config = new Config();
        config.setString(Keys.IOTM_PERMANENT_OUTPUT_CONTROL, "true");
        encoder = inject(new IotmProtocolEncoder(null, config));

        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0801010142"),
                ((MqttPublishMessage) encoder.encodeCommand(command)).payload());

        command.set(Command.KEY_INDEX, 2);
        command.set(Command.KEY_DATA, 0);
        encoder = inject(new IotmProtocolEncoder(null, config));

        verifyFrame(binary("0202080079df0d8648700000040800ffffff7f0901010042"),
                ((MqttPublishMessage) encoder.encodeCommand(command)).payload());

    }

}
