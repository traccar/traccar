package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class StarLinkProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        StarLinkProtocolDecoder decoder = new StarLinkProtocolDecoder(null);

        decoder.setFormat("#IMEI#,#EDT#,#PDT#,#LAT#,#LONG#,#SPD#,#IGN#,#ODO#,#DUR#,#TDUR#,#LAC#,#CID#,#VIN#,#VBAT#,#EID#,#EDSC#,#DRV#,#SATU#,#CSS#,#OUT1#,#OUT2#");

        verifyAttribute(decoder, text(
                "$SLU862549048472545,06,25,862549048472545,200304085936,200304085937,+4126.1828,+00209.8472,013.9,0,000000,,1,2120,6306,14.452,03.980,33,External Device,0,9,67,0,0,7,0,137,13,2,5625,-11,-20,99*1F"),
                Position.KEY_IGNITION, false);

        decoder.setFormat("#EDT#,#EID#,#PDT#,#LAT#,#LONG#,#SPD#,#HEAD#,#ODO#,#IN1#,#IN2#,#IN3#,#IN4#,#OUT1#,#OUT2#,#OUT3#,#OUT4#,#LAC#,#CID#,#VIN#,#VBAT#,#DEST#,#IGN#,#ENG#");

        verifyAttributes(decoder, text(
                "$SLU068328,06,55,170518122023,16,,,,,,000000,1,1,0,0,0,0,0,0,10443,32722,12.664,03.910,,0,0,,01000001FDB3A9*BF"));

        verifyAttributes(decoder, text(
                "$SLU068328,06,56,170518122023,20,,,,,,000000,1,1,0,0,0,0,0,0,10443,32722,12.664,03.910,,0,0,2,01000001FDB3A9,00000000000000000000*D9"));

        verifyAttributes(decoder, text(
                "$SLU068328,06,57,170518122038,01,,,,,,000000,1,1,0,0,1,0,0,0,10443,32722,12.669,03.910,,0,0,0,99*6E"));

        verifyAttributes(decoder, text(
                "$SLU068328,06,58,170518122045,19,,,,,,000000,1,1,0,0,1,0,0,0,10443,32722,12.678,03.910,,0,0*7C"));

        verifyAttributes(decoder, text(
                "$SLU068328,06,59,170518122054,16,,,,,,000000,1,1,0,0,0,0,0,0,10443,32723,12.678,03.910,,0,0,01000001FDB3A9,01000001ACE0A6*BF"));

        verifyPosition(decoder, text(
                "$SLU031B2B,06,622,170329035057,01,170329035057,+3158.0018,+03446.6968,004.9,007,000099,1,1,0,0,0,0,0,0,,,14.176,03.826,,1,1,1,4*B0"));

        verifyPosition(decoder, text(
                "$SLU031B2B,06,624,170329035143,01,170329035143,+3158.0171,+03446.6742,006.8,326,000099,1,1,0,0,0,0,0,0,10452,8723,14.212,03.827,,1,1,1,4*6D"));

        verifyPosition(decoder, text(
                "$SLU0330D5,06,3556,170314063523,19,170314061634,+3211.7187,+03452.8106,000.0,332,015074,1,1,0,0,0,0,0,0,10443,32722,12.870,03.790,,0,0*FC"));

        verifyPosition(decoder, text(
                "$SLU0330D5,06,3555,170314063453,20,170314061634,+3211.7187,+03452.8106,000.0,332,015074,1,1,0,0,0,0,0,0,10443,32722,12.838,03.790,,0,0,1,,1122*74"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375153,170117051824,01,170117051823,+3203.2073,+03448.1360,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,12.655,04.085,,0,0,0,99*45"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375155,170117052615,24,170117052613,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.290,04.083,,1,1*5B"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375156,170117052616,34,170117052614,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.277,04.084,1,1,1,1*F3"));

        verifyPosition(decoder, text(
                "$SLU006968,06,375154,170117052613,04,170117052612,+3203.2079,+03448.1369,000.0,300,085725,1,1,0,0,0,0,0,0,10422,36201,14.287,04.084,,1,0*5B"));

        decoder.setFormat("#EDT#,#EID#,#PDT#,#LAT#,#LONG#,#SPD#,#HEAD#,#ODO#,#LAC#,#CID#,#VIN#,#VBAT#");

        verifyPosition(decoder, text(
                "$SLU352353083185436,06,85,190527214903,01,190527214903,+0614.1883,-07535.5033,000.0,000,000082.505,5070,50473,0,12.148,03.507,,100,0.02,35.0,1,1513,60,1,99*30"));

    }

}
