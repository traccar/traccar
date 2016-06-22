package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TelicProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TelicProtocolDecoder decoder = new TelicProtocolDecoder(new TelicProtocol());

        verifyPosition(decoder, text(
                "003097061325,220616044200,0,220616044200,247169,593911,3,48,248,8,,,50,1024846,,1111,00,48,0,51,0406"));

        verifyPosition(decoder, text(
                "003097061325,210216112630,0,210216001405,246985,594078,3,0,283,12,,,23,4418669,,0010,00,117,0,0,0108"));

        verifyPosition(decoder, text(
                "592078222222,010100030200,0,240516133500,222222,222222,3,0,0,5,,,37,324,,1010,00,48,0,0,0406"));

        verifyPosition(decoder, text(
                "002017297899,220216111100,0,220216111059,014306446,46626713,3,7,137,7,,,448,266643,,0000,00,0,206,0,0407"));

        verifyPosition(decoder, text(
                "003097061325,210216112630,0,210216001405,246985,594078,3,0,283,12,,,23,4418669,,0010,00,117,0,0,0108"));

        verifyNothing(decoder, text(
                "0026970613|248|01|004006011"));

        verifyPosition(decoder, text(
                "032097061399,210216112800,0,210216112759,246912,594076,3,47,291,10,,,46,4419290,,0010,00,100,0,0,0108"));

        verifyPosition(decoder, text(
                "002017297899,190216202500,0,190216202459,014221890,46492170,3,0,0,6,,,1034,43841,,0000,00,0,209,0,0407"));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7"),
                position("2013-06-27 04:16:52.000", true, 47.53410, 16.66530));

        verifyPosition(decoder, text(
                "182043672999,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7"));

    }

}
