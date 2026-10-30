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
                position().attribute(Position.KEY_POWER, 11.834));

        verifyDecode(decoder, text(
                "~000000381FFFFFFF;202606-00156-0050342;860879072759885;2564;R10Maquete;1783529657604;-19.88243;-43.97854;849.37;B;0.09;28;99;0.52;78.55;1;1;0.00;0.00;126;1;2789491369;12152;196;Arqi;0;1783960543474;2;1;2789491369;040B000100007030;J-R10;0$"),
                position().attribute(Position.KEY_DRIVER_UNIQUE_ID, "2789491369"));

        verifyDecode(decoder, text(
                "~000000041FECFFFF;202510-00107-0020127;868695060706247;7164;JR12WEG;1764792191981;-26.48141;-49.05476;19.50;B;0.00;13;31;2.43;168.47;0;0;125;1;0;3493;VIVO;4;1764792191998;1;0;0;0.00$"),
                position().attribute(Position.KEY_ALARM, Position.ALARM_LOW_BATTERY));

        verifyDecode(decoder, text(
                "~0000000000008176;123456789012345;1234;1705687209759;-19.87417;-43.96894;11;0$"),
                position());

    }

}
