package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class JmakProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new JmakProtocolDecoder(null));

        verifyDecode(decoder, buffer(
                "~000000380FE3FFFF;777777-77777-7777555;860693086800738;183;Test1;1793131375854;-19.88247;-43.97855;837.88;B;0.02;33;28;0.47;0.00;1;0;12.08;0.59;11834;0;0;4;1793131375941;2;1;040C000000643939;J-R07;180$"),
                position()
                        .location("2026-10-27T20:02:55.854Z", true, -19.88247, -43.97855)
                        .deviceTime("2026-10-27T20:02:55.941Z")
                        .attribute(Position.KEY_INDEX, 183L)
                        .attribute("nickname", "Test1")
                        .attribute(Position.KEY_IGNITION, true)
                        .attribute(Position.KEY_HOURS, 43488000L)
                        .attribute(Position.KEY_ODOMETER, 590.0)
                        .attribute(Position.KEY_POWER, 11.834)
                        .attribute(Position.PREFIX_IN + 1, true)
                        .attribute(Position.KEY_VERSION_FW, "4.12.0.0-d99")
                        .attribute("model", "J-R07")
                        .attribute("tripId", 180L));

        verifyDecode(decoder, buffer(
                "~0000003C1FEFFFFF;202511-00112-0022779;868695060670146;374472;BT-2567;1786448351530;-19.92171;-44.02875;937.60;B;1.34;23;99;0.60;101.95;1;0;1654.88;12145.22;114;1;28887;3966;Arqi;4;1786448993540;1;1;0;0.00;040B010100007430;J-R12;0|00000058D98F585F;36127.09;3846.95;0.00;1408.50;41;0;L;13.10;85;42;65;1.52;368;0;0;112;35;7;22;24;27.25;0.00;$"),
                position()
                        .location("2026-08-11T11:39:11.530Z", true, -19.92171, -44.02875)
                        .attribute(Position.KEY_EVENT, 114)
                        .attribute(Position.KEY_POWER, 28.887)
                        .attribute(Position.KEY_BATTERY, 3.966)
                        .attribute(Position.KEY_OPERATOR, "Arqi")
                        .attribute(Position.PREFIX_ADC + 1, 0.0)
                        .attribute(Position.KEY_VERSION_FW, "4.11.1.1-t0")
                        .attribute(Position.KEY_OBD_ODOMETER, 36127090.0)
                        .attribute(Position.KEY_HOURS, 13849020000L)
                        .attribute(Position.KEY_RPM, 1408.5)
                        .attribute("canStatus", 41)
                        .attribute(Position.KEY_FUEL_CONSUMPTION, 13.1)
                        .attribute("oilTemp", 85.0)
                        .attribute(Position.KEY_ENGINE_LOAD, 65.0)
                        .attribute("oilPressure", 368.0)
                        .attribute("adBlueLevel", 7.0)
                        .attribute("batteryVoltage", 27.25));

        verifyDecode(decoder, buffer(
                "~000000381FE3FFFF;202605-00000-0002201;863238076469644;96473;BT-2567-R2;1786448603045;-19.92165;-44.02896;936.49;B;0.00;30;99;0.47;321.42;1;0;581.00;4390.17;29391;539;TIMB;4;1786449426156;1;1;0;040B010100007430;J-R22;0|00000058D98FD85F;36127.12;3847.00;0.70;591.00;44;0;L;3.85;84;13.62;33;58;1.46;180;0;0;100;40;7;22;24;27.60;0.00;$"),
                position()
                        .attribute(Position.KEY_ODOMETER, 4390170.0)
                        .attribute(Position.KEY_OBD_SPEED, 0.7)
                        .attribute(Position.KEY_RPM, 591.0)
                        .attribute("tripFuelUsed", 13.62)
                        .attribute("model", "J-R22"));

        verifyDecode(decoder, buffer(
                "~000000381FEFFFFF;202605-00000-0002201;863238076469644;94016;BT-2567-R2;1786137837003;-19.88223;-43.97861;0.00;3;0.00;0;99;99.00;0.00;1;1;566.25;4309.49;113;1;24519;533;0;0;1786449612625;0;1;0;040B010100007430;J-R22;0|0000000000008013;36042.83;3834.25;0;0.00;$"),
                position()
                        .location("2026-08-07T21:23:57.003Z", false, -19.88223, -43.97861)
                        .attribute(Position.KEY_ARCHIVE, true)
                        .attribute(Position.KEY_EVENT, 113)
                        .attribute("eventStatus", 1)
                        .attribute("canStatus", 0));

        verifyDecode(decoder, buffer(
                "~0000003F9FE3FFFF;202511-00112-0022779;868695060670146;370824;BT-2567;1786137412871;-19.88246;-43.97861;823.90;B;0.00;23;99;0.87;289.52;0;1;1640.20;12064.50;25751;3989;Arqi;4;1786449179991;1;0;0;5;1786135960005;1786137412870;0.00;040B010100007430;J-R12;0$"),
                position()
                        .attribute(Position.KEY_IGNITION, false)
                        .attribute(Position.KEY_HOURS, 5904720000L)
                        .attribute(Position.KEY_ODOMETER, 12064500.0)
                        .attribute("model", "J-R12"));

        verifyDecode(decoder, buffer(
                "~000000041FECFFFF;202510-00107-0020127;868695060706247;7164;JR12WEG;1764792191981;-26.48141;-49.05476;19.50;B;0.00;13;31;2.43;168.47;0;0;125;1;0;3493;VIVO;4;1764792191998;1;0;0;0.00$"),
                position()
                        .attribute(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY)
                        .attribute(Position.KEY_BATTERY, 3.493));

        verifyDecode(decoder, buffer(
                "~0000000000008176;123456789012345;1234;1705687209759;-19.87417;-43.96894;11;0$"),
                position()
                        .attribute(Position.KEY_INDEX, 1234L)
                        .attribute(Position.KEY_ARCHIVE, false));

        verifyDecode(decoder, buffer(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8224;NULL;1750689928520;-19.88245;-43.97853;837.10;B;0.06;18;31;0.89;0.00;1;0;52.50;4.74;11750;3875;TIMB;4;1750689928534;1;9;0;0.00|00000000000041DF;180.00;211.00;208.00;190.00;7;50;P;80;69$"),
                position()
                        .location("2025-06-23T14:45:28.520Z", true, -19.88245, -43.97853)
                        .attribute(Position.KEY_POWER, 11.75)
                        .attribute(Position.KEY_BATTERY, 3.875)
                        .attribute(Position.PREFIX_OUT + 2, true)
                        .attribute(Position.KEY_OBD_ODOMETER, 180000.0)
                        .attribute(Position.KEY_FUEL_LEVEL, 80.0)
                        .attribute("oilTemp", 69.0));

        verifyDecode(decoder, buffer(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8208;NULL;1750689688530;-19.88247;-43.97855;830.20;B;0.61;16;31;0.79;0.00;1;0;52.43;4.74;11750;3863;TIMB;4;1750689688544;1;9;0;0.00|000000000000405C;0.00;0.00;0;0;0$"),
                position());

        verifyDecode(decoder, buffer(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8210;NULL;1750689718530;-19.88246;-43.97855;825.60;B;0.53;16;31;0.87;0.00;1;0;52.45;4.74;11750;3863;TIMB;4;1750689718543;1;9;0;0.00$"),
                position());

        verifyDecode(decoder, binary(
                "5e000000381fe3ffff01e0f22d0079ff2d40747c0003157534c6807e00004a7900000000000000000000000001a17ef56c8b001e569500431b1f0145150b0000081f1c00310000000100000000152200000000452df00eec0000003004000001a17ef56cb902010000000000000000040c0000006439390000004a2d5232320000000024"),
                position()
                        .location("2026-10-27T20:02:55.755Z", true, -19.88245, -43.97855)
                        .deviceTime("2026-10-27T20:02:55.801Z")
                        .attribute(Position.KEY_INDEX, 19065L)
                        .attribute(Position.KEY_SATELLITES, 31)
                        .attribute(Position.KEY_IGNITION, true)
                        .attribute(Position.KEY_HOURS, 194760000L)
                        .attribute(Position.KEY_ODOMETER, 690L)
                        .attribute(Position.KEY_POWER, 11.76)
                        .attribute(Position.KEY_BATTERY, 3.82)
                        .attribute(Position.PREFIX_IN + 1, true)
                        .attribute(Position.KEY_VERSION_FW, "4.12.0.0-d99")
                        .attribute("model", "J-R22")
                        .attribute("tripId", 0L));

        verifyDecode(decoder, binary(
                "5E000000007FEFFFFF0316A12D0000212D0028E200007048860DDF79000004D2000000000000000000000000018D22E1571F001E53590043175E01484D0B0017D30C1F00790069910000000012D6870000671011830133F410680054494D040000018D22E1571F0203000000000012D68707313233343536370000000000007DDF000012D68700006710110004D201E240000B38253200DDD54B04D2000001E2408324"),
                position()
                        .location("2024-01-19T18:00:09.759Z", true, -19.87417, -43.96894)
                        .attribute(Position.KEY_EVENT, 131)
                        .attribute(Position.KEY_DRIVER_UNIQUE_ID, "1234567")
                        .attribute(Position.KEY_OPERATOR, "TIM")
                        .attribute("serial", "31323334353637")
                        .attribute(Position.KEY_OBD_ODOMETER, 12345670.0)
                        .attribute(Position.KEY_RPM, 1234.56)
                        .attribute(Position.KEY_FUEL_LEVEL, 50.0)
                        .attribute(Position.KEY_FUEL_CONSUMPTION, 12.34)
                        .attribute(Position.KEY_FUEL_USED, 1234.56));

        verifyDecode(decoder, binary(
                "5E000000000000817600007048860DDF79000004D20000018D22E1571F001E53590043175E0B0024"),
                position()
                        .attribute(Position.KEY_INDEX, 1234L)
                        .attribute(Position.KEY_ARCHIVE, false));

        verifyDecode(decoder, binary(
                "5e000000041fe3ffff038f752d0082352d32dcd500031612d4831608000029740000000000000000000000000197b14b5c40001e569900431b21014a780b00003b131f0044000000010000000017ed00000001e82df90f3054494d420400000197b14b5c4d010100000000000000000000000000000000405c0000000000000000002824"),
                position()
                        .location("2025-06-27T12:09:55.520Z", true, -19.88249, -43.97857)
                        .attribute(Position.KEY_POWER, 11.769)
                        .attribute(Position.KEY_BATTERY, 3.888)
                        .attribute("oilTemp", 0.0));

        verifyDecode(decoder, buffer(
                "{\"cnt\":18976,\"s_num\":\"123122-31231-4224124\",\"ts\":1793128788285,\"gps\":[-19.88247,-43.97855],\"alt\":839.95,\"speed\":0.00,\"nsat\":30,\"hdop\":0.49,\"azi\":0.00,\"ive\":0,\"rssi\":28,\"evt\":{\"id\":113,\"st\":0},\"bckup\":0,\"hourmt\":53.48,\"odomt\":0.69,\"tup\":1,\"fix\":2,\"ope\":\"TIMB\",\"tec\":4,\"bat\":3704,\"vin\":11750,\"ios\":0,\"name\":\"\",\"imei\":868018070913150,\"tag\":0,\"ver\":\"040C000000643939\",\"mod\":\"J-R22\",\"trp_id\":0,\"ts_n\":1793128788327}"),
                position()
                        .location("2026-10-27T19:19:48.285Z", true, -19.88247, -43.97855)
                        .deviceTime("2026-10-27T19:19:48.327Z")
                        .attribute(Position.KEY_INDEX, 18976L)
                        .attribute(Position.KEY_EVENT, 113)
                        .attribute(Position.KEY_HOURS, 192528000L)
                        .attribute(Position.KEY_ODOMETER, 690.0)
                        .attribute(Position.KEY_POWER, 11.75)
                        .attribute(Position.KEY_BATTERY, 3.704)
                        .attribute(Position.KEY_VERSION_FW, "4.12.0.0-d99")
                        .attribute("model", "J-R22"));

        verifyDecode(decoder, buffer(
                "{\"s_num\":\"202401-00033-0010466\",\"imei\":123456789012345,\"cnt\":1234,\"name\":\"teste1\",\"ts\":1705687209759,\"gps\":[-19.87417,-43.96894],\"alt\":840.45,\"speed\":60.99,\"nsat\":12,\"rssi\":31,\"hdop\":1.21,\"azi\":270.25,\"ive\":1,\"bckup\":0,\"hourmt\":12345.67,\"odomt\":67543.21,\"vin\":13300,\"bat\":4200,\"ope\":\"TIM\",\"tec\":4,\"ts_n\":1705687209759,\"fix\":1,\"tup\":1,\"evt\":{\"id\":126,\"st\":1,\"name\":\"12345678\"},\"tag\":1234567,\"trp\":\"31323334353637\",\"can\":{\"1\":12.34,\"2\":1234.56,\"3\":67543.21,\"4\":\"P50\",\"5\":91,\"6\":56,\"7\":7,\"9\":567.89,\"10\":\"H12.34\",\"11\":12345.67,\"14\":1234.56}}"),
                position()
                        .location("2024-01-19T18:00:09.759Z", true, -19.87417, -43.96894)
                        .attribute(Position.KEY_EVENT, 126)
                        .attribute(Position.KEY_CARD, "12345678")
                        .attribute(Position.KEY_DRIVER_UNIQUE_ID, "12345678")
                        .attribute(Position.KEY_OBD_SPEED, 12.34)
                        .attribute(Position.KEY_RPM, 1234.56)
                        .attribute(Position.KEY_FUEL_LEVEL, 50.0)
                        .attribute("oilTemp", 91.0)
                        .attribute(Position.KEY_THROTTLE, 56.0)
                        .attribute("autonomy", 567.89)
                        .attribute(Position.KEY_FUEL_CONSUMPTION, 12.34)
                        .attribute(Position.KEY_FUEL_USED, 1234.56));

        verifyDecode(decoder, buffer(
                "{\"cnt\":10610,\"s_num\":\"233333-33333-3333333\",\"ts\":1751026130545,\"gps\":[-19.88249,-43.97855],\"alt\":850.40,\"speed\":0.00,\"nsat\":18,\"hdop\":0.87,\"azi\":0.00,\"ive\":0,\"rssi\":31,\"evt\":{},\"bckup\":0,\"hourmt\":61.25,\"odomt\":4.88,\"tup\":1,\"fix\":1,\"ope\":\"TIMB\",\"tec\":4,\"bat\":3882,\"vin\":0,\"ios\":0,\"name\":\"\",\"imei\":868695060715016,\"tag\":0,\"anin\":0.00,\"ts_n\":1751026130554}"),
                position()
                        .location("2025-06-27T12:08:50.545Z", true, -19.88249, -43.97855)
                        .attribute(Position.KEY_INDEX, 10610L)
                        .attribute(Position.KEY_BATTERY, 3.882));

        verifyDecode(decoder, buffer(
                "*000000000000FFFF;860693086800738;777777-77777-7777555;139;0;0;1793130085347;1793131313664;-19.88246;-19.88246;-43.97852;-43.97857;1111;0.59;0.59;11.73;12.07;$"),
                position()
                        .location("2026-10-27T20:01:53.664Z", true, -19.88246, -43.97857)
                        .attribute("tripId", 139L)
                        .attribute("tripStart", 1793130085347L)
                        .attribute("tripEnd", 1793131313664L)
                        .attribute(Position.KEY_ODOMETER, 590.0)
                        .attribute(Position.KEY_ODOMETER_TRIP, 0.0));

        verifyDecode(decoder, buffer(
                "*000000000003FFFF;868018070913275;202605-00000-0000701;99601;0;0;1786449067726;1786451352195;-19.92171;-19.88237;-44.02885;-43.97903;1111;4389.30;4400.96;581.05;581.68;1;101:0;$"),
                position()
                        .location("2026-08-11T12:29:12.195Z", true, -19.88237, -43.97903)
                        .attribute("tripId", 99601L)
                        .attribute("tripStart", 1786449067726L)
                        .attribute(Position.KEY_ODOMETER, 4400960.0));

        verifyDecode(decoder, buffer(
                "$868695060037460;ok;#"),
                position(Checks.ATTRIBUTES)
                        .attribute(Position.KEY_RESULT, "$868695060037460;ok;#"));

    }

}
