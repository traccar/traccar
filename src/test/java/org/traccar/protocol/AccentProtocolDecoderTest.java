package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class AccentProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AccentProtocolDecoder(null));

        verifyDecode(decoder, text(
                "AA011.0.103R10, ICC:8921602050440128136F, IMEI:861001002935274"),
                position(Checks.ATTRIBUTES)
                        .outdated(true)
                        .attribute(Position.KEY_TYPE, "PNA")
                        .attribute(Position.KEY_VERSION_FW, "1.0.103R10")
                        .attribute(Position.KEY_ICCID, "8921602050440128136F"));

        verifyDecode(decoder, text(
                "861001002935274 AA00124427@14/01/27,12:44:20+00 ,ICC:8921602050440128136F, IMEI:861001002935274"),
                position(Checks.ATTRIBUTES)
                        .outdated(true)
                        .attribute(Position.KEY_TYPE, "INF")
                        .attribute(Position.KEY_ICCID, "8921602050440128136F"));

        verifyDecode(decoder, text(
                "AAAA"));

        verifyDecode(decoder, text(
                "860141077743871 AA23010279022CB3190099432C0011122A18FFFFFFFF800000000000010000000026D70B14"),
                position().location("2026-09-24T18:22:49.000Z", true, 36.80644, 10.07367)
                        .attribute(Position.KEY_BATTERY, 4.2));

        verifyDecode(decoder, text(
                "861001002935274 AA1300ADCA01F0CE94009CB42D0000212A00FD01FEE3801300000000071739875B1B8C"
                        + "0000000000000000000000000000000C1A"),
                position().location("2018-12-16T12:21:30.000Z", true, 32.93123, 10.44957)
                        .attribute(Position.KEY_RPM, 0)
                        .attribute(Position.KEY_COOLANT_TEMP, -40)
                        .attribute(Position.KEY_SATELLITES, 12));

    }

}
