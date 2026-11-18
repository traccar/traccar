package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class IterProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new IterProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "28280100180005035254407167747100200205020500010432000088bd"));

        verifyDecode(decoder, binary(
                "282812004716815cf817b503fd09f682fac900bb000a000000000802d4001012d50000114733046f11810565"
                        + "000000007a24f36cf36c0713046000000000000000000000000000317c390000"),
                position().location("2019-06-05T19:27:49.000+00:00", true, -27.599288, -48.605763)
                        .attribute(Position.KEY_BATTERY, 4.481));

        verifyDecode(decoder, binary(
                "2828140032000a5df0f85e03fd09f61ffac900a3000e000000000802d40005060c000028963187046b00000"
                        + "072000433bc1155030c0000"),
                position().attribute(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY)
                        .attribute(Position.KEY_BATTERY, 4.437));

        verifyDecode(decoder, binary(
                "282815003200345defaf8903fd09f63bfac900ad0005000000000702d40005060c000028963b01046f000000"
                        + "7000031fa1115505670000"),
                position().attribute(Position.KEY_IGNITION, true)
                        .attribute(Position.KEY_BATTERY, 4.437));

        verifyDecode(decoder, binary(
                "28283b002b001d63e5387003fd09ff75fac8a87c0000000000360502d4000604180000cfc83e047f11a205"
                        + "080000001e"),
                position().attribute("idle", true)
                        .attribute("idleTime", 30)
                        .attribute(Position.KEY_BATTERY, 4.514));

        verifyDecode(decoder, binary(
                "282803000400070188"),
                position(Checks.ATTRIBUTES));

        verifyDecode(decoder, binary(
                "28288000110102010000000652454c41592c31204f4b"),
                position(Checks.ATTRIBUTES)
                        .attribute("instructionUid", 6L)
                        .attribute(Position.KEY_RESULT, "RELAY,1 OK"));
    }

}
