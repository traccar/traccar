package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class MegastekProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        MegastekProtocolDecoder decoder = new MegastekProtocolDecoder(new MegastekProtocol());

        verify(decoder.decode(null, null,
                "STX,GerAL22,$GPRMC,174752.000,A,3637.060059,S,6416.2354,W,0.00,0.00,030812,,,A*55,F,,imei:861785000249353,05,180.6,Battery=100%,,1,722,310,0FA6,39D0;8F"));

        verify(decoder.decode(null, null,
                "STX,GerAL22,$GPRMC,000051.000,A,3637.079590,S,6416.2148,W,1.72,332.98,010109,,,A*52,L,,imei:861785000249353,03,275.3,Battery=68%,,1,722,07,0515,1413;41"));
        
        verify(decoder.decode(null, null,
                "STX,,$GPRMC,001339.000,A,4710.85395,N,02733.58209,E,1.65,238.00,010109,,,A*67,L,Help,imei:013227009737796,0/8,137.1,Battery=100%,,0,226,01,2B9B,BBBF;8D"));

        verify(decoder.decode(null, null,
                "STX,102110830074542,$GPRMC,114229.000,A,2238.2024,N,11401.9619,E,0.00,0.00,310811,,,A*64,F,LowBattery,imei:012207005553885,03,113.1,Battery=24%,,1,460,01,2531,647E;57"));

        verify(decoder.decode(null, null,
                "STX863070014949464   $GPRMC,215942.290,A,4200.1831,N,02128.5904,E,003.1,079.8,090813,,,A*6E,294,02,0064,0F3D,18,17,0000,000000,0000,0.00,0.02,0.00,Store;D8"));

        verify(decoder.decode(null, null,
                "STX123456            $GPRMC,063709.000,A,2238.1998,N,11401.9670,E,0.00,,250313,,,A*7F,460,01,2531,647E,11,87,1000,001001,0000,0.00,0.02,0.00,Timer;4A"));

        verify(decoder.decode(null, null,
                "STX260475            $GPRMC,104032.001,A,4022.1119,N,01811.4081,E,000.0,000.0,060913,,,A*67,222,01,815A,D455,11,99,0000,0001,0,Timer;"));

        verify(decoder.decode(null, null,
                "LOGSTX,123456789012345,$GPRMC,225419.000,A,3841.82201,N,09494.73357,W,12.46,135.33,270914,,,A*47,F,,imei:123456789012345,0/6,,Battery=100%,,0,,,5856,78A3;24"));

        verify(decoder.decode(null, null,
                "LOGSTX,123456789012345,$GPRMC,230551.000,A,3841.81956,N,09494.45403,W,0.00,0.00,270914,,,A*7C,L,,imei:123456789012345,0/7,269.7,Battery=100%,,0,,,5856,78A3;83"));

        verify(decoder.decode(null, null,
                "LOGSTX,123456789012345,$GPRMC,230739.000,A,3841.81895,N,09494.12409,W,0.00,0.00,270914,,,A*70,L,,imei:123456789012345,0/7,269.7,Battery=100%,,0,,,5856,78A3;78"));
        
    }

}
