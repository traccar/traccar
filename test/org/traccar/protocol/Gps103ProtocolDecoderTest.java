package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.traccar.helper.DecoderVerifier.verify;

public class Gps103ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gps103ProtocolDecoder decoder = new Gps103ProtocolDecoder(null);

        // Log on request
        assertNull(decoder.decode(null, null, null, "##,imei:359586015829802,A"));

        // Heartbeat package
        assertNull(decoder.decode(null, null, null, "359586015829802"));

        // No GPS signal
        assertNull(decoder.decode(null, null, null, "imei:359586015829802,tracker,000000000,13554900601,L,;"));
        
        verify(decoder.decode(null, null, null,
                "imei:869039001186913,tracker,1308282156,0,F,215630.000,A,5602.11015,N,9246.30767,E,1.4,,175.9,"));

        verify(decoder.decode(null, null, null,
                "imei:359710040656622,tracker,13/02/27 23:40,,F,125952.000,A,3450.9430,S,13828.6753,E,0.00,0"));
        
        verify(decoder.decode(null, null, null,
                "imei:359710040565419,tracker,13/05/25 14:23,,F,062209.000,A,0626.0411,N,10149.3904,E,0.00,0"));

        verify(decoder.decode(null, null, null,
                "imei:353451047570260,tracker,1302110948,,F,144807.000,A,0805.6615,S,07859.9763,W,0.00,,"));
        
        verify(decoder.decode(null, null, null,
                "imei:359587016817564,tracker,1301251602,,F,080251.000,A,3223.5832,N,11058.9449,W,0.03,"));
        
        verify(decoder.decode(null, null, null,
                "imei:359587016817564,tracker,1301251602,,F,080251.000,A,3223.5832,N,11058.9449,W,,"));

        verify(decoder.decode(null, null, null,
                "imei:012497000208821,tracker,1301080525,,F,212511.000,A,2228.5279,S,06855.6328,W,18.62,268.98,"));

        verify(decoder.decode(null, null, null,
                "imei:012497000208821,tracker,1301072224,,F,142411.077,A,2227.0739,S,06855.2912,,0,0,"));

        verify(decoder.decode(null, null, null,
                "imei:012497000431811,tracker,1210260609,,F,220925.000,A,0845.5500,N,07024.7673,W,0.00,,"));

        verify(decoder.decode(null, null, null,
                "imei:100000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,"));

        verify(decoder.decode(null, null, null,
                "imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,"));

        verify(decoder.decode(null, null, null,
                "imei:353451042861763,tracker,1106132241,,F,144114.000,A,2301.9052,S,04909.3676,W,0.13,"));

        verify(decoder.decode(null, null, null,
                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,"));

        verify(decoder.decode(null, null, null,
                "imei:353451049926460,tracker,1208042043,123456 99008026,F,124336.000,A,3509.8668,N,03322.7636,E,0.00,,"));

        // SOS alarm
        verify(decoder.decode(null, null, null,
                "imei:359586015829802,help me,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Low battery alarm
        verify(decoder.decode(null, null, null,
                "imei:359586015829802,low battery,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Geo-fence alarm
        verify(decoder.decode(null, null, null,
                "imei:359586015829802,stockade,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Move alarm
        verify(decoder.decode(null, null, null,
                "imei:359586015829802,move,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Over speed alarm
        verify(decoder.decode(null, null, null,
                "imei:359586015829802,speed,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        verify(decoder.decode(null, null, null,
                "imei:863070010423167,tracker,1211051840,,F,104000.000,A,2220.6483,N,11407.6377,,0,0,"));

        verify(decoder.decode(null, null, null,
                "imei:863070010423167,tracker,1211051951,63360926,F,115123.000,A,2220.6322,N,11407.5313,E,0.00,,"));

        verify(decoder.decode(null, null, null,
                "imei:863070010423167,tracker,1211060621,,F,062152.000,A,2220.6914,N,11407.5506,E,15.85,347.84,"));
        
        verify(decoder.decode(null, null, null,
                "imei:863070012698733,tracker,1303092334,,F,193427.000,A,5139.0369,N,03907.2791,E,0.00,,"));
        
        verify(decoder.decode(null, null, null,
                "imei:869039001186913,tracker,130925065533,0,F,065533.000,A,5604.11015,N,9232.12238,E,0.0,,329.0,"));
        
        verify(decoder.decode(null, null, null,
                "imei:359710041641581,acc alarm,1402231159,,F,065907.000,A,2456.2591,N,06708.8335,E,7.53,76.10,,1,0,0.03%,,"));
        
        verify(decoder.decode(null, null, null,
                "imei:359710041641581,acc alarm,1402231159,,F,065907.000,A,2456.2591,N,06708.8335,E,7.53,76.10,,1,0,0.03%,,"));
        
        verify(decoder.decode(null, null, null,
                "imei:313009071131684,tracker,1403211928,,F,112817.000,A,0610.1133,N,00116.5840,E,0.00,,,0,0,0.0,0.0,"));
        
        verify(decoder.decode(null, null, null,
                "imei:866989771979791,tracker,140527055653,,F,215653.00,A,5050.33113,N,00336.98783,E,0.066,0"));
        
        verify(decoder.decode(null, null, null,
                "imei:353552045375005,tracker,150401165832,61.0,F,31.0,A,1050.73696,N,10636.49489,E,8.0,,22.0,"));
        
        verify(decoder.decode(null, null, null,
                "imei:353552045403597,tracker,150420050648,53.0,F,0.0,A,N,5306.64155,E,00700.77848,0.0,,1.0,;"));
        
        verify(decoder.decode(null, null, null,
                "imei:353552045403597,tracker,150420051153,53.0,F,0.0,A,5306.64155,N,00700.77848,E,0.0,,1.0,;"));
        
        verify(decoder.decode(null, null, null,
                "imei:359710047424644,tracker,150506224036,,F,154037.000,A,0335.2785,N,09841.1543,E,3.03,337.54,,0,0,45.16%,,;"));

    }

}
