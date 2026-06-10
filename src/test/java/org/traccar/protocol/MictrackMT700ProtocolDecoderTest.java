package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class MictrackMT700ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MictrackMT700ProtocolDecoder(null));

        // GPS available, LBS OFF
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815$GPRMC,123318.00,A,2238.8946,N,11402.0635,E,,,100124,,,A*5C\n"));

        // GPS available, LBS ON
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#$GPRMC,123548.00,A,2238.8936,N,11402.0640,E,,,100124,,,A*5A\n"));

        // GPS unavailable, LBS ON
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#460,00,1D29,156153D$GPRMC,121831.00,V,,,,,,,100124,,,A*7C\n"));

        // WiFi, LBS OFF, AGPS ON
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815$WIFI,124517.00,A,-39,6877248FA31A,-39,7E77248FA31A,-73,DC333DF82C74,-75,0260736CF982,-77,90769F421140,100124*0E\n"));

        // WiFi, LBS ON, AGPS ON
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#460,00,262C,11F1$WIFI,022300.00,A,-31,6877248FA31A,-32,7E77248FA31A,-73,0260736CF982,-74,DC333DF82C74,-74,90769F421140,100124*74\n"));

        // SHAKE alarm
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#SHAKE#1\n#3815$GPRMC,090000.00,A,2238.8946,N,11402.0635,E,0.0,0.0,100124,,,A*00\n"));

        // MT700W variant header
        verifyAttributes(decoder, text(
                "#862255061947757#MT700W#0000#AUTO#1\n#3815#460,00,262C,11F1$WIFI,095147.00,V,,,,,,,,,,,241223*06\n"));

    }

}
