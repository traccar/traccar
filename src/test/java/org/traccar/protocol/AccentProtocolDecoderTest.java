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
                        .attribute(Position.KEY_ICCID, "8921602050440128136F")
                        .attribute("imei", "861001002935274"));

        verifyDecode(decoder, text(
                "860141077743871 AA00124427@14/01/27,12:44:20+00"),
                position(Checks.ATTRIBUTES)
                        .outdated(true)
                        .attribute(Position.KEY_TYPE, "INF"));

        verifyDecode(decoder, text(
                "861001002935274 AA00124427@14/01/27,12:44:20+00 ,ICC:8921602050440128136F, IMEI:861001002935274"),
                position(Checks.ATTRIBUTES)
                        .outdated(true)
                        .attribute(Position.KEY_TYPE, "INF")
                        .attribute(Position.KEY_ICCID, "8921602050440128136F")
                        .attribute("imei", "861001002935274"));

        verifyDecode(decoder, text(
                "AAAA"));

        verifyDecode(decoder, text(
                "860141077743871 AA01MF2"));

        verifyDecode(decoder, text(
                "860141077743871 AA23010279022CB3190099432C0011122A18FFFFFFFF800000000000010000000026D70B14"),
                position().location("2026-09-24T18:22:49.000Z", true, 36.80644, 10.07367)
                        .attribute(Position.KEY_BATTERY, 4.2));

        verifyDecode(decoder, text(
                "861001002935274 AA2101171A022D833D009A2BAA0000A0000000006380FC000000000400000000001458"),
                position().location("2014-01-01T19:50:50.000Z", false, -36.89525, -10.17287)
                        .attribute(Position.KEY_BATTERY, 0.0)
                        .attribute(Position.KEY_ODOMETER, 4000L));

        verifyDecode(decoder, text(
                "861001002935274 AA1300ADCA01F0CE94009CB42D0000212A00FD01FEE3801300000000071739875B1B8C"
                        + "0000000000000000000000000000000C1A"),
                position().location("2018-12-16T12:21:30.000Z", true, 32.93123, 10.44957)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_RPM, 0)
                        .attribute(Position.KEY_COOLANT_TEMP, -40)
                        .attribute(Position.KEY_SATELLITES, 12));

        verifyDecode(decoder, text(
                "860141077743871 AA3300AF34022CB38C00994290000D1E2A000001FE67800000000000030000000026FA"
                        + "000000000000000000000000000000091C"),
                position().location("2026-10-28T12:27:32.000Z", true, 36.80663, 10.07341)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 9));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEE5022CB3BB009942140010222A0000000067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:13.000Z", true, 36.80671, 10.07321)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEE8022CB3B10099420100111E2A00FF010067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:16.000Z", true, 36.80670, 10.07318)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEEA022CB3A00099420D00100D2A00FF000067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:18.000Z", true, 36.80667, 10.07320)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEED022CB3800099423D0010092A00FF010067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:21.000Z", true, 36.80661, 10.07328)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEEF022CB37600994251000B192A00FF000067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:23.000Z", true, 36.80660, 10.07331)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA2300AEF1022CB37400994260000C222A00FE010067800000000000030000000026FA"
                        + "000000000000000000000000000000071A"),
                position().location("2026-10-28T12:26:25.000Z", true, 36.80659, 10.07333)
                        .attribute(Position.KEY_BATTERY, 4.2)
                        .attribute(Position.KEY_SATELLITES, 7));

        verifyDecode(decoder, text(
                "860141077743871 AA01MF2_XS"));

    }

}
