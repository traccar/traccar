package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class JmakProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new JmakProtocolDecoder(null));

        verifyDecode(decoder, text(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8224;NULL;1750689928520;-19.88245;-43.97853;837.10;B;0.06;18;31;0.89;0.00;1;0;52.50;4.74;11750;3875;TIMB;4;1750689928534;1;9;0;0.00|00000000000041DF;180.00;211.00;208.00;190.00;7;50;P;80;69$"),
                position());

        verifyDecode(decoder, text(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8208;NULL;1750689688530;-19.88247;-43.97855;830.20;B;0.61;16;31;0.79;0.00;1;0;52.43;4.74;11750;3863;TIMB;4;1750689688544;1;9;0;0.00|000000000000405C;0.00;0.00;0;0;0$"),
                position());

        verifyDecode(decoder, text(
                "~000000041FE3FFFF;233333-33333-3333333;868695060715016;8210;NULL;1750689718530;-19.88246;-43.97855;825.60;B;0.53;16;31;0.87;0.00;1;0;52.45;4.74;11750;3863;TIMB;4;1750689718543;1;9;0;0.00$"),
                position());

        verifyDecode(decoder, text(
                "~000000380FE3FFFF;777777-77777-7777555;860693086800738;183;Test1;1793131375854;-19.88247;-43.97855;837.88;B;0.02;33;28;0.47;0.00;1;0;12.08;0.59;11834;0;0;4;1793131375941;2;1;040C000000643939;J-R07;180$"),
                position()
                        .location("2026-10-27T20:02:55.854Z", true, -19.88247, -43.97855)
                        .deviceTime("2026-10-27T20:02:55.941Z")
                        .attribute(Position.KEY_INDEX, 183L)
                        .attribute("nickname", "Test1")
                        .attribute(Position.KEY_IGNITION, true)
                        .attribute(Position.KEY_HOURS, 43488000.0)
                        .attribute(Position.KEY_ODOMETER, 590.0)
                        .attribute(Position.KEY_POWER, 11.834)
                        .attribute(Position.KEY_INPUT, 1)
                        .attribute(Position.KEY_OUTPUT, 0)
                        .attribute(Position.KEY_VERSION_FW, "4.12.0.0-d99")
                        .attribute("model", "J-R07")
                        .attribute("tripId", 180L));

        verifyDecode(decoder, text(
                "~0000003C1FEFFFFF;202511-00112-0022779;868695060670146;374472;BT-2567;1786448351530;-19.92171;-44.02875;937.60;B;1.34;23;99;0.60;101.95;1;0;1654.88;12145.22;114;1;28887;3966;Arqi;4;1786448993540;1;1;0;0.00;040B010100007430;J-R12;0|00000058D98F585F;36127.09;3846.95;0.00;1408.50;41;0;L;13.10;85;42;65;1.52;368;0;0;112;35;7;22;24;27.25;0.00;$"),
                position()
                        .location("2026-08-11T11:39:11.530Z", true, -19.92171, -44.02875)
                        .attribute(Position.KEY_EVENT, 114)
                        .attribute(Position.KEY_POWER, 28.887)
                        .attribute(Position.KEY_BATTERY, 3.966)
                        .attribute(Position.KEY_OPERATOR, "Arqi")
                        .attribute(Position.PREFIX_ADC + 1, 0.0)
                        .attribute(Position.KEY_VERSION_FW, "4.11.1.1-t0"));

        verifyDecode(decoder, text(
                "~000000381FE3FFFF;202605-00000-0002201;863238076469644;96473;BT-2567-R2;1786448603045;-19.92165;-44.02896;936.49;B;0.00;30;99;0.47;321.42;1;0;581.00;4390.17;29391;539;TIMB;4;1786449426156;1;1;0;040B010100007430;J-R22;0|00000058D98FD85F;36127.12;3847.00;0.70;591.00;44;0;L;3.85;84;13.62;33;58;1.46;180;0;0;100;40;7;22;24;27.60;0.00;$"),
                position()
                        .attribute(Position.KEY_ODOMETER, 4390170.0)
                        .attribute("model", "J-R22"));

        verifyDecode(decoder, text(
                "~000000381FEFFFFF;202605-00000-0002201;863238076469644;94016;BT-2567-R2;1786137837003;-19.88223;-43.97861;0.00;3;0.00;0;99;99.00;0.00;1;1;566.25;4309.49;113;1;24519;533;0;0;1786449612625;0;1;0;040B010100007430;J-R22;0|0000000000008013;36042.83;3834.25;0;0.00;$"),
                position()
                        .location("2026-08-07T21:23:57.003Z", false, -19.88223, -43.97861)
                        .attribute(Position.KEY_ARCHIVE, true)
                        .attribute(Position.KEY_EVENT, 113)
                        .attribute("eventStatus", 1));

        verifyDecode(decoder, text(
                "~0000003F9FE3FFFF;202511-00112-0022779;868695060670146;370824;BT-2567;1786137412871;-19.88246;-43.97861;823.90;B;0.00;23;99;0.87;289.52;0;1;1640.20;12064.50;25751;3989;Arqi;4;1786449179991;1;0;0;5;1786135960005;1786137412870;0.00;040B010100007430;J-R12;0$"),
                position()
                        .attribute(Position.KEY_IGNITION, false)
                        .attribute(Position.KEY_HOURS, 5904720000.0)
                        .attribute(Position.KEY_ODOMETER, 12064500.0)
                        .attribute("model", "J-R12"));

        verifyDecode(decoder, text(
                "~000000041FECFFFF;202510-00107-0020127;868695060706247;7164;JR12WEG;1764792191981;-26.48141;-49.05476;19.50;B;0.00;13;31;2.43;168.47;0;0;125;1;0;3493;VIVO;4;1764792191998;1;0;0;0.00$"),
                position()
                        .attribute(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY)
                        .attribute(Position.KEY_BATTERY, 3.493));

        verifyDecode(decoder, text(
                "~0000000000008176;123456789012345;1234;1705687209759;-19.87417;-43.96894;11;0$"),
                position()
                        .attribute(Position.KEY_INDEX, 1234L)
                        .attribute(Position.KEY_ARCHIVE, false));

    }

}
