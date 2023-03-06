package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FutureWayProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FutureWayProtocolDecoder(null));

        verifyNull(decoder, text(
                "410000003F2000020,IMEI:354828100126461,battery level:6,network type:7,CSQ:236F42"));

        verifyNull(decoder, text(
                "41000000622000020,IMEI:354828100002140,battery level:32,network type:7,CSQ:18,Version:FWS03.APP.SVN144.202010231442"));

        verifyPosition(decoder, text(
                "4100000092A00004\r\nIMEI:354828100002140\r\nGPS:A,201102090140,2233.246582N,11356.300781E,0.000,0.000\r\nWIFI:\r\nLBS:460,0,9763,219992654\r\nbattery level:32\r\nSteps:0\r\n4F42"));

        verifyPosition(decoder, text(
                "410000009BA00004GPS:V,200902093333,0.000000N,0.000000E,0.000,0.000\r\nWIFI:3,1|90-67-1C-F7-21-6C|52&2|80-89-17-C6-79-A0|54&3|40-F4-20-EF-DD-2A|58\r\nLBS:460,0,46475066,69\r\n6A42"));

    }

}
