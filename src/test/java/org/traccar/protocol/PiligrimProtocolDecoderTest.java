package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PiligrimProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PiligrimProtocolDecoder(null));
        
        verifyPositions(decoder, request(HttpMethod.POST,
                "/bingps?imei=868204005544720&csq=18&vout=00&vin=4050&dataid=00000000",
                binary("fff2200d4110061a32354f3422310062000a0005173b0000a101000300005e00fff2200d4110100932354f2b22310042000b000e173b00009f01000700006000")));

        verifyPosition(decoder, request(HttpMethod.POST,
                "/push.do",
                buffer("&phoneNumber=%2B+78000000000&message=ALARM KEY; $GPRMC,180752.000,A,5314.0857,N,03421.8173,E,0.00,104.74,220722,,,A,V* 29,05; GSM: 250-01 0b54-0519,1c30,3e96,3ebe,412e 25;  S; Batt: 405,M")));

    }

}
