package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Gl200ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Gl200ProtocolDecoder decoder = new Gl200ProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTFRI,04040C,359231038939904,,,10,1,2,0.0,117,346.0,8.924243,50.798077,20130618122040,0262,0002,0299,109C,00,0.0,,,,,,,,,20130618122045,00F6"));
        
        assertNotNull(decoder.decode(null, null,
                "+RESP:GTSTT,04040C,359231038939904,,42,0,0.0,117,346.0,8.924243,50.798077,20130618125152,0262,0002,0299,109C,00,20130618125154,017A"));
        
        assertNotNull(decoder.decode(null, null,
                "+RESP:GTFRI,020102,000035988863964,,0,0,1,1,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTFRI,020102,135790246811220,,0,0,1,1,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTFRI,020102,135790246811220,,0,0,2,1,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,0,4.3,92,70.0,121.354335,31.222073,20090101000000,0460,0000,18d8,6141,00,,20090214093254,11F0"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTDOG,020102,135790246811220,,0,0,1,1,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,2000.0,20090214093254,11F0"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTLBC,020102,135790246811220,,+8613800000000,1,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,,20090214093254,11F0"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTGCR,020102,135790246811220,,3,50,180,2,0.4,296,-5.4,121.391055,31.164473,20100714104934,0460,0000,1878,0873,00,,20100714104934,000C"));

        assertNotNull(decoder.decode(null, null,
                "+RESP:GTFRI,07000D,868487001005941,,0,0,1,1,0.0,0,46.3,-77.039627,38.907573,20120731175232,0310,0260,B44B,EBC9,0015e96913a7,-58,,100,20120731175244,0114"));

    }

}
