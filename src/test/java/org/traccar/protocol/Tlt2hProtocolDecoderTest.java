package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class Tlt2hProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Tlt2hProtocolDecoder(null));

        verifyPositions(decoder, false, text(
                "#862255061752835#MT710#0000#AUTO#1\r\n" +
                "#4106#$GPRMC,151410.00,A,3010.4103,N,08146.2728,W,,214.90,010324,,,A*58\r\n"));

        verifyPositions(decoder, text(
                "#868105044690301#MT600+#0000#0#0#143#40#0#AUTO#1\r\n",
                "#072030fcf21$GPRMC,155616.00,A,4931.9210,N,09652.5290,W,53.80,90.00,150224,,,A*48\r\n"));

        verifyAttribute(decoder, text(
                "#867665041689485#MT700N#0000#HT#1\r\n",
                "#5065$GPRMC,148996.00,A,2485.2458,N,01258.4535,E,,,151348,,,A*5D\r\n"),
                Position.KEY_BATTERY, 5.065);

        verifyPositions(decoder, false, text(
                "#862255061983166#MT700NW#0000#TOWED#1\r\n",
                "#4502$WIFI,051550.00,A,-50,7683C2CBC0B0,-51,7683C29BC0B0,-51,7683C2BBC0B0,-51,7483C2DBC0B0,-51,7683C2ABC0B0,221123*78\r\n"));

        verifyPositions(decoder, false, text(
                "#862255061825896#MT710#0000#TOWED#1\r\n",
                "#39#$WIFI,015259.00,A,-47,7483C2DBC0B0,-48,7683C2ABC0B0,-48,7683C29BC0B0,-48,7683C2CBC0B0,-48,7683C2BBC0B0,151123*74\r\n"));

        verifyPositions(decoder, false, text(
                "#860517049471362#MT700#0000#AUTO#1\r\n",
                "#36$GPRMC,,V,,,,,,,,,,A*5C\r\n"));

        verifyPositions(decoder, text(
                "#867198059727390#MT700#0000#AUTO#1\r\n",
                "#38$GPRMC,105721.00,A,2238.3071,N,11401.7575,E,,96.70,250321,,,A*74\r\n"));

        verifyPositions(decoder, false, text(
                "#867198059727390#MT700#0000#AUTO#1\r\n",
                "#40$WIFI,123532.00,A,-62,D8325A0ABADD,-64,EC172F8965BC,-64,7405A5D457D4,260321*07\r\n"));

        verifyPositions(decoder, text(
                "#860425040088567#MT600+#0000#0#1#129#40#0#AUTOLOW#1\r\n",
                "#000321901$GPRMC,172030.00,A,4845.2906,N,01910.2742,E,0.01,,041219,,,A*43\r\n"));

        verifyAttribute(decoder, text(
                "#869260042149724#MP90_4G#0000#AUTOLOW#1\r\n" +
                "#02201be0000$GPRMC,001645.00,A,5333.2920,N,11334.3857,W,0.03,,250419,,,A*5E\r\n"),
                Position.KEY_IGNITION, false);

        verifyPositions(decoder, text(
                "#867962040161955#MT600#0000#0#0#137#41#0#AUTO#1\r\n" +
                "#00019023402$GPRMC,084702.00,A,3228.6772,S,11545.9684,E,,159.80,251018,,,A*56\r\n"));

        verifyPositions(decoder, text(
                "#868323028789359#MT600#0000#AUTOLOW#1\r\n",
                "#07d8cd5198$GPRMC,164934.00,A,1814.4854,N,09926.0566,E,0.03,,240417,,,A*4A\r\n"));

        verifyNull(decoder, text(
                "#861075026000000#\r\n",
                "#0000#AUTO#1\r\n",
                "#002c4968045$GPRMC,001556.00,A,3542.1569,N,13938.9814,E,7.38,185.71,160417,,,A*55\r\n"));

        verifyPositions(decoder, text(
                "#863835026938048#MT500#0000#AUTO#1\r\n",
                "#67904917c0e$GPRMC,173926.00,A,4247.8476,N,08342.6996,W,0.03,,160417,,,A*59\r\n"));

        verifyPositions(decoder, text(
                "#357671030108689##0000#AUTO#1\r\n",
                "#13AE2F8F$GPRMC,211452.000,A,0017.378794,S,03603.441981,E,0.000,0,060216,,,A*68\r\n"));

        verifyPositions(decoder, text(
                "#357671030946351#V500#0000#AUTO#1\r\n",
                "#$GPRMC,223835.000,A,0615.3545,S,10708.5779,E,14.62,97.41,070313,,,D*70\r\n"),
                position("2013-03-07 22:38:35.000", true, -6.25591, 107.14297));

        verifyPositions(decoder, text(
                "\r\n#357671030946351#V500#0000#AUTO#1\r\n",
                "#$GPRMC,223835.000,A,0615.3545,S,10708.5779,E,14.62,97.41,070313,,,D*70\r\n"));

        verifyPositions(decoder, text(
                "#357671030938911#V500#0000#AUTOSTOP#1\r\n",
                "#00b34d3c$GPRMC,140026.000,A,2623.6452,S,02828.8990,E,0.00,65.44,130213,,,A*4B\r\n"));

        verifyPositions(decoder, text(
                "#123456789000001#V3338#0000#SMS#3\r\n",
                "#25ee0dff$GPRMC,083945.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E\r\n",
                "#25ee0dff$GPRMC,083950.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E\r\n",
                "#25ee0dff$GPRMC,083955.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E"));

        verifyPositions(decoder, text(
                "#353686009063310#353686009063310#0000#AUTO#2\r\n",
                "#239757a9$GPRMC,150252.001,A,2326.6856,S,4631.8154,W,,,260513,,,A*52\r\n",
                "#239757a9$GPRMC,150322.001,A,2326.6854,S,4631.8157,W,,,260513,,,A*55"));

        verifyPositions(decoder, text(
                "#357671031289215#V600#0000#AUTOLOW#1\r\n",
                "#00735e1c$GPRMC,115647.000,A,5553.6524,N,02632.3128,E,0.00,0.0,130614,0.0,W,A*28"));

        verifyPositions(decoder, false, text(
                "#860186058100000#MT700#0000#AUTO#1\r\n",
                "#39#262,03,8CE6,A672$WIFI,154928.00,A,-74,3CA62F52615B,-82,A0E4CB83852D,,,050123*28\r\n##\r\n"));

        verifyPositions(decoder, false, text(
                "#860186058100000#MT700#0000#AUTO#1\r\n",
                "#39#262,03,8CE6,A672$GPRMC,115419.00,V,,,,,,,050123,,,A*7D\r\n##\r\n"));
    }

}
