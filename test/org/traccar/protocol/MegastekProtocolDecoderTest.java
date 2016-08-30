package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MegastekProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        MegastekProtocolDecoder decoder = new MegastekProtocolDecoder(new MegastekProtocol());

        verifyPosition(decoder, text(
                "$MGV002,869152024446923,,S,290816,200627,V,5056.21059,N,00439.25034,E,00,00,00,99.9,,,-25.1,,206,01,0BBB,4418,28,,,,,,,,,01,093,Timer;"));

        verifyPosition(decoder, text(
                "$MGV002,869152024446923,869152024446923,S,240816,151631,A,5053.83335,N,00424.05702,E,00,10,00,0.88,2.645,76.09,22.7,,206,01,07D1,6600,28,,,,,,,,,01,100,Timer;!"));

        verifyPosition(decoder, text(
                "STX,013950007137061,$GPRMC,191959.000,A,5203.09602,N,00830.77057,E,5.73,255.27,240716,,,A*62,L,Belt Up,imei:013950007137061,0/5,,Battery=52%,,1,262,03,0084,B20E;FD"));

        verifyPosition(decoder, text(
                "STX,865067021328417,$GPRMC,064721.000,A,4241.2793,N,02321.9762,E,6.74,346.90,300316,,,1*CA,F,Nil-Alarms,imei:865067021328417,9,559.8,Battery=82%,0,284,03,047E,2B5F;99"));

        verifyNothing(decoder, text(
                "0147$MGV002,354550050292669,GVT900,S,141215,031804,A,,S,,E,00,04,00,5.17,0.000,193.05,117.1,0.0,510,10,041B,0A5E,,0000,0000,22,12,0, , ,,1-1,98,Timer;!"));

        verifyNothing(decoder, text(
                "0137$MGV002,354550050292669,GVT900,R,141215,031804,A,,S,,E,00,03,00,3.67,0.000,0.00,117.1,0.0,510,10,,,,0000,0000,22,12,0, , ,,1-1,98,PW ON;!"));

        verifyPosition(decoder, text(
                "0125$MGV002,860719020193193,DeviceName,R,240214,104742,A,2238.20471,N,11401.97967,E,00,03,00,1.20,0.462,356.23,137.9,1.5,460,07,262C,0F54,25,0000,0000,0,0,0,28.5,28.3,,,100,Timer;"));

        verifyPosition(decoder, text(
                "$MGV002,860719020193193,DeviceName,R,240214,104742,A,2238.20471,N,11401.97967,E,00,03,00,1.20,0.462,356.23,137.9,1.5,460,07,262C,0F54,25,0000,0000,0,0,0,28.5,28.3,,,100,Timer;!"),
                position("2014-02-24 10:47:42.000", true, 22.63675, 114.03299));

        verifyPosition(decoder, text(
                "STX2010101801      j$GPRMC,101053.000,A,2232.7607,N,11404.7669,E,0.00,,231110,,,A*7F,460,00,2795,0E6A,14,94,1000,0000,91,Timer;1D"));

        verifyPosition(decoder, text(
                "STX,861001005215757,$GPRMC,180118.000,A,4241.330116,N,2321.931251,E,0.00,182.19,130915,,E,A,F,Nil-Alarms,imei:861001005215757,8,577.0,Battery=38%,0,284,03,03E8,3139;7A"));

        verifyPosition(decoder, text(
                "STX,865067020439090,$GPRMC,171013.000,A,5919.1411,N,01804.1681,E,0.000,294.41,140815,,,A"));

        verifyPosition(decoder, text(
                "$MGV002,013777007536434,,R,010114,000057,V,0000.0000,N,00000.0000,E,00,00,00,99.9,0.000,0.00,0.0,80.263,510,89,2342,030B,,0000,0000,200,96,0, , ,,,,Timer;!"));

        verifyPosition(decoder, text(
                "STX,GerAL22,$GPRMC,174752.000,A,3637.060059,S,6416.2354,W,0.00,0.00,030812,,,A*55,F,,imei:861785000249353,05,180.6,Battery=100%,,1,722,310,0FA6,39D0;8F"));

        verifyPosition(decoder, text(
                "STX,GerAL22,$GPRMC,000051.000,A,3637.079590,S,6416.2148,W,1.72,332.98,010109,,,A*52,L,,imei:861785000249353,03,275.3,Battery=68%,,1,722,07,0515,1413;41"));
        
        verifyPosition(decoder, text(
                "STX,,$GPRMC,001339.000,A,4710.85395,N,02733.58209,E,1.65,238.00,010109,,,A*67,L,Help,imei:013227009737796,0/8,137.1,Battery=100%,,0,226,01,2B9B,BBBF;8D"));

        verifyPosition(decoder, text(
                "STX,102110830074542,$GPRMC,114229.000,A,2238.2024,N,11401.9619,E,0.00,0.00,310811,,,A*64,F,LowBattery,imei:012207005553885,03,113.1,Battery=24%,,1,460,01,2531,647E;57"));

        verifyPosition(decoder, text(
                "STX863070014949464   $GPRMC,215942.290,A,4200.1831,N,02128.5904,E,003.1,079.8,090813,,,A*6E,294,02,0064,0F3D,18,17,0000,000000,0000,0.00,0.02,0.00,Store;D8"));

        verifyPosition(decoder, text(
                "STX123456            $GPRMC,063709.000,A,2238.1998,N,11401.9670,E,0.00,,250313,,,A*7F,460,01,2531,647E,11,87,1000,001001,0000,0.00,0.02,0.00,Timer;4A"));

        verifyPosition(decoder, text(
                "STX260475            $GPRMC,104032.001,A,4022.1119,N,01811.4081,E,000.0,000.0,060913,,,A*67,222,01,815A,D455,11,99,0000,0001,0,Timer;"));

        verifyPosition(decoder, text(
                "LOGSTX,123456789012345,$GPRMC,225419.000,A,3841.82201,N,09494.73357,W,12.46,135.33,270914,,,A*47,F,,imei:123456789012345,0/6,,Battery=100%,,0,,,5856,78A3;24"));

        verifyPosition(decoder, text(
                "LOGSTX,123456789012345,$GPRMC,230551.000,A,3841.81956,N,09494.45403,W,0.00,0.00,270914,,,A*7C,L,,imei:123456789012345,0/7,269.7,Battery=100%,,0,,,5856,78A3;83"));

        verifyPosition(decoder, text(
                "LOGSTX,123456789012345,$GPRMC,230739.000,A,3841.81895,N,09494.12409,W,0.00,0.00,270914,,,A*70,L,,imei:123456789012345,0/7,269.7,Battery=100%,,0,,,5856,78A3;78"));
        
    }

}
