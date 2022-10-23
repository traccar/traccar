package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class SolarPoweredProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SolarPoweredProtocolDecoder(null));

        verifyAttribute(decoder, binary(
                "7e850256553309440011003e81131914030600332301a61ed709209ff40014b89082020f0283100000f908000000440000003d1f19021784114161726f6e34475630312d323030333031057e"),
                Position.KEY_RSSI, 23);

        verifyPosition(decoder, binary(
                "7e850256553304728011003e811319130b0b11211f01a2e6be091fa0e10114cc1582020f00831000004e7400000044000000223819020c84114161726f6e34475630312d313931303331127e"));

        verifyPosition(decoder, binary(
                "7e850256553304728011003e811319130b0d160e2901a2e66f091fa0ab0014c39482020f0083100002f42c00000287000000fc2719021484114161726f6e34475630312d313931303331e67e"));

    }

}
