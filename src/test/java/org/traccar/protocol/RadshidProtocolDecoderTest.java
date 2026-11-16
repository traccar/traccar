package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class RadshidProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RadshidProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "600000000114fcfb1602010100000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a"));

        verifyDecode(decoder, binary(
                "600000000114fcfb160201e0000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a010001f3000000000000005777ab640000000000000000000000000000a000137ee8441ecb3f96063b00000a0100657a"),
                position()
                        .location("2016-07-02T11:54:12Z", true, 32.7084100, 51.6636566)
                        .altitude(1595)
                        .attribute(Position.KEY_EVENT, 0xE0)
                        .attribute(Position.KEY_CHARGE, true)
                        .attribute(Position.KEY_SATELLITES, 10),
                position()
                        .attribute(Position.KEY_EVENT, 0xF3));

        verifyDecode(decoder, binary(
                "A800000001001F60CA0201FC0000000000000063959BB800000000000080000000000000008000150C37A81C0427AEFF9A00000D0924076000040000007802000014000000CA0100007702432277640000400600002C0100003401FC0000000000000063959BBD00000000000080000000000000008000150C37A81C0427AEFF9A00000C0A24076000050000007902000014000000CA0100000000432277640000400600002C010000682E6E"),
                position()
                        .location("2022-12-11T08:58:32Z", true, 35.3122216, 47.003435)
                        .attribute(Position.KEY_EVENT, 0xFC)
                        .attribute(Position.KEY_DISTANCE, 0L)
                        .attribute(Position.KEY_VERSION_FW, 96)
                        .attribute(Position.KEY_INDEX, 4L)
                        .attribute("uptime", 632L)
                        .attribute("bootCount", 20)
                        .attribute("simCredit", 458)
                        .attribute("accOffDuration", 631)
                        .attribute(Position.KEY_RSSI, 67)
                        .attribute(Position.KEY_POWER, 11.9)
                        .attribute(Position.KEY_BATTERY_LEVEL, 100),
                position()
                        .attribute(Position.KEY_INDEX, 5L));

        verifyDecode(decoder, binary(
                "67010000010001e2400301e0000000000000006afad4203c0046000000800004d200000000c000154763401ea3048004b0005a090c490d830005000000100e000007000200dc050000000050227c5f00020000cc4100002042ab004e3113c125e42200b00100000b00000034120000cdab0000320000001e000000baffffff01eb000000000000006afad45c3c0046000000800004d200000000a001000000000000000004b0005a090c490d830005000000100e000007000200dc050000000050227c5f0002000070c20000a0c1ab004e3113c125e422000000000000000000000000000000000000000000000000000000000001d8000000000000006afad4983c0046000000800004d200000000c000154763401ea3048004b0005a090c490d830005000000100e000007000200dc050000000050627c5f00020000cc4100002042ab014e3113c125e42200b00100000b00000034120000cdab0000320000001e000000baffffff0cfb"),
                position()
                        .location("2026-11-16T10:00:00Z", true, 35.7, 51.4)
                        .speed(UnitsConverter.knotsFromKph(60))
                        .course(90)
                        .altitude(1200)
                        .attribute(Position.KEY_EVENT, 0xE0)
                        .attribute(Position.KEY_DISTANCE, 1234L)
                        .attribute(Position.KEY_PDOP, 1.2)
                        .attribute(Position.KEY_VERSION_FW, 131)
                        .attribute("bootCount", 7)
                        .attribute("flashBacklog", 2)
                        .attribute("startupType", 2)
                        .attribute(Position.PREFIX_TEMP + 1, 25.5)
                        .attribute(Position.KEY_HUMIDITY, 40.0)
                        .attribute("lastKnownPosition", false)
                        .attribute("partialIccid", "9821000012345678")
                        .network(network().cell(cell().mcc(432).mnc(11).lac(0x1234).cid(0xABCD).signal(-70))),
                position(Checks.ATTRIBUTES)
                        .outdated(true)
                        .deviceTime("2026-11-16T10:01:00Z")
                        .attribute(Position.KEY_ALARM, Position.ALARM_SOS)
                        .attribute(Position.PREFIX_TEMP + 1, null)
                        .attribute(Position.KEY_HUMIDITY, null),
                position()
                        .location("2026-11-16T10:02:00Z", false, 35.7, 51.4)
                        .speed(0)
                        .attribute(Position.KEY_SATELLITES, null)
                        .attribute("lastKnownPosition", true)
                        .attribute(Position.KEY_ARCHIVE, true));

    }

}
