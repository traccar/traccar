package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class WialonProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new WialonProtocolDecoder(null));

        verifyNull(decoder, text(
                "#L#2.0;42001300083;;CE45"));

        verifyPosition(decoder, text(
                "#SD#300924;154245;5554.350052;N;3644.670410;E;2.92;NA;NA;NA;7A01"));

        verifyAttribute(decoder, text(
                "#D#220323;114150;2234.80479;N;11354.87786;E;0;NA;59;11;NA;NA;NA;;NA;d_battr:1:94,d_csq:1:21,di_light:1:1;E7C9"),
                "di_light", 1.0);

        verifyAttributes(decoder, text(
                "#D#NA;NA;5429.681944502211763;N;02654.60403650999069;E;NA;NA;NA;NA;NA;NA;NA;1.0;NA;m1:1:9196679,d1:1:15397,t1:1:20,b1:1:162,fuel1:2:21588.0,pv1:2:35.98,finish:1:1;0x9b0"));

        verifyAttributes(decoder, text(
                "#D#120319;112003;NA;NA;NA;NA;0.000;NA;NA;0;NA;NA;NA;NA;NA;101_521347:1:521246,101_158:1:510,101_521055:1:510,101_521055_2.9:1:509,101_521056:1:3;626B"));

        verifyNull(decoder, text(
                "#L#123456789012345;test"));
        
        verifyNull(decoder, text(
                "#L#2002;NA"));
        
        verifyNull(decoder, text(
                "#P#"));

        verifyPosition(decoder, text(
                "#D#101118;061143;0756.0930;N;12338.6403;E;18.223;99.766;-4.000;10;0.800;NA;NA;NA;NA;101_521347:1:521249,101_521126:1:6593598,101_521127:1:774780,101_521072_21.1:1:0,101_521072_21.2:1:71353;F24A"));

        verifyPosition(decoder, text(
                "2.0;99999999#D#101118;061143;0756.0930;N;12338.6403;E;18.223;99.766;-4.000;10;0.800;NA;NA;NA;NA;101_521347:1:521249,101_521126:1:6593598,101_521127:1:774780,101_521072_21.1:1:0,101_521072_21.2:1:71353;F24A"));

        verifyPosition(decoder, text(
                "#D#151216;135910;5321.1466;N;04441.7929;E;87;156;265.000000;12;1.000000;241;NA;NA;NA;odo:2:0.000000,total_fuel:1:430087,can_fls:1:201,can_taho:1:11623,can_mileage:1:140367515"));

        verifyPosition(decoder, text(
                "#D#151216;140203;5312.59514;N;04830.37834;E;53;273;NA;10;NA;NA;NA;NA;NA;EvId:1:1,Board:2:12.81,Accum:2:4.28"));

        verifyPosition(decoder, text(
                "#SD#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4"),
                position("2013-04-27 20:56:01.000", true, 55.74338, 37.66139));

        verifyPosition(decoder, text(
                "99999999#SD#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4"),
                position("2013-04-27 20:56:01.000", true, 55.74338, 37.66139));

        verifyPosition(decoder, text(
                "#SD#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5"));

        verifyPosition(decoder, text(
                "#D#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4;0.0;0;0;14.77,0.02,3.6;NA;count1:1:564,fuel:2:45.8,hw:3:V4.5"));
        
        verifyPosition(decoder, text(
                "#D#190114;051312;4459.6956;N;04105.9930;E;35;306;204.000000;12;NA;452986639;NA;106.000000;NA;sats_gps:1:9,sats_glonass:1:3,balance:2:12123.000000,stay_balance:1:0"));
        
        verifyPosition(decoder, text(
                "#D#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5;1.74;NA;NA;NA;NA;NA"));

        verifyPosition(decoder, text(
                "#D#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5;1.74;NA;NA;;NA;NA"));

        verifyPositions(decoder, text(
                "#B#080914;073235;5027.50625;N;03026.19321;E;0.700;0.000;NA;4;NA;NA;NA;;NA;Батарея:3:100 %|080914;073420;5027.50845;N;03026.18854;E;1.996;292.540;NA;4;NA;NA;NA;;NA;Батарея:3:100 %"));
        
        verifyPositions(decoder, text(
                "#B#110914;102132;5027.50728;N;03026.20369;E;1.979;288.170;NA;NA;NA;NA;NA;;NA;Батарея:3:100 %"));

        verifyPositions(decoder, text(
                "#B#110315;045857;5364.0167;N;06127.8262;E;0;155;965;7;2.40;4;0;;NA;Uacc:2:3.4,Iacc:2:0.000,Uext:2:13.2,Tcpu:2:14.4,Balance:2:167.65,GPS:3:Off"));

        verifyPositions(decoder, text(
                "#B#110315;045857;5364.0167;N;06127.8262;E;0;155;965;7;2.40;4;0;14.77,0.02,3.6;AB45DF01145;"));

        verifyAttribute(decoder, text(
                "#D#120319;112003;NA;NA;NA;NA;0.000;NA;NA;0;NA;NA;NA;NA;NA;motion:3:false"),
                "motion", false);

    }

}
