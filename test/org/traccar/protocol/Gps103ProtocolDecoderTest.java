package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class Gps103ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gps103ProtocolDecoder decoder = new Gps103ProtocolDecoder(new TestDataManager());

        // Log on request
        //assertNull(decoder.decode(null, null, "##,imei:359586015829802,A"));

        // Heartbeat package
        //assertNull(decoder.decode(null, null, "359586015829802"));

        // No GPS signal
        //assertNull(decoder.decode(null, null, "imei:359586015829802,tracker,000000000,13554900601,L,;"));

        assertNotNull(decoder.decode(null, null,
                "imei:012497000208821,tracker,1301080525,,F,212511.000,A,2228.5279,S,06855.6328,W,18.62,268.98,"));

        assertNotNull(decoder.decode(null, null,
                "imei:012497000208821,tracker,1301072224,,F,142411.077,A,2227.0739,S,06855.2912,,0,0,"));

        assertNotNull(decoder.decode(null, null,
                "imei:012497000431811,tracker,1210260609,,F,220925.000,A,0845.5500,N,07024.7673,W,0.00,,"));

        assertNotNull(decoder.decode(null, null,
                "imei:100000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451042861763,tracker,1106132241,,F,144114.000,A,2301.9052,S,04909.3676,W,0.13,"));

        assertNotNull(decoder.decode(null, null,
                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,"));

        assertNotNull(decoder.decode(null, null,
                "imei:353451049926460,tracker,1208042043,123456 99008026,F,124336.000,A,3509.8668,N,03322.7636,E,0.00,,"));

        // SOS alarm
        assertNotNull(decoder.decode(null, null,
                "imei:359586015829802,help me,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Low battery alarm
        assertNotNull(decoder.decode(null, null,
                "imei:359586015829802,low battery,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Geo-fence alarm
        assertNotNull(decoder.decode(null, null,
                "imei:359586015829802,stockade,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Move alarm
        assertNotNull(decoder.decode(null, null,
                "imei:359586015829802,move,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        // Over speed alarm
        assertNotNull(decoder.decode(null, null,
                "imei:359586015829802,speed,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

        assertNotNull(decoder.decode(null, null,
                "imei:863070010423167,tracker,1211051840,,F,104000.000,A,2220.6483,N,11407.6377,,0,0,"));

        assertNotNull(decoder.decode(null, null,
                "imei:863070010423167,tracker,1211051951,63360926,F,115123.000,A,2220.6322,N,11407.5313,E,0.00,,"));

        assertNotNull(decoder.decode(null, null,
                "imei:863070010423167,tracker,1211060621,,F,062152.000,A,2220.6914,N,11407.5506,E,15.85,347.84,"));
    }

}
