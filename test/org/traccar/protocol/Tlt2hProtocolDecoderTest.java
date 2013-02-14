package org.traccar.protocol;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class Tlt2hProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Tlt2hProtocolDecoder decoder = new Tlt2hProtocolDecoder(null);
        decoder.setDataManager(new TestDataManager());
        
        assertNotNull(decoder.decode(null, null,
                "#357671030938911#V500#0000#AUTOSTOP#1\r\n" +
                "#00b34d3c$GPRMC,140026.000,A,2623.6452,S,02828.8990,E,0.00,65.44,130213,,,A*4B\r\n"));

        assertNotNull(decoder.decode(null, null,
                "#123456789000001#V3338#0000#SMS#3\r\n" +
                "#25ee0dff$GPRMC,083945.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E\r\n" +
                "#25ee0dff$GPRMC,083950.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E\r\n" +
                "#25ee0dff$GPRMC,083955.180,A,2233.4249,N,11406.0046,E,0.00,315.00,251207,,,A*6E"));

    }

}
