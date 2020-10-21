package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FutureWayProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FutureWayProtocolDecoder decoder = new FutureWayProtocolDecoder(null);

        verifyNull(decoder, text(
                "410000003F2000020,IMEI:354828100126461,battery level:6,network type:7,CSQ:236F42"));

        verifyPosition(decoder, text(
                "410000009BA00004GPS:V,200902093333,0.000000N,0.000000E,0.000,0.000\r\nWIFI:3,1|90-67-1C-F7-21-6C|52&2|80-89-17-C6-79-A0|54&3|40-F4-20-EF-DD-2A|58\r\nLBS:460,0,46475066,69\r\n6A42"));

    }

}
