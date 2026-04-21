package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PolteProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PolteProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"_id\":\"5f75cf7b02c5023bfc0beaf7\",\"location\":{\"LocationMetrics\":{\"EnvironmentDensity\":1,\"LocationType\":2,\"carrierInfo\":{\"aux\":{\"PLMN\":\"310410\",\"country\":\"United States\",\"name\":\"ATT Wireless Inc\"},\"crs\":{\"PLMN\":\"310410\",\"country\":\"United States\",\"name\":\"ATT Wireless Inc\"}},\"hdop\":1850000,\"leversion\":\"2.2.18-20200729T140651\",\"towercount\":1},\"altitude\":0.0011297669261693954,\"confidence\":783.7972188868215,\"detected_at\":1601556342,\"latitude\":29.77368956725161,\"longitude\":-98.26530342694024,\"towerDB\":\"default\",\"ueToken\":\"ALT12503DE04336CB2E3A4A113FCDE05DF05A6F\",\"uid\":\"WZuDMv5Je\"},\"report\":{\"battery\":{\"count\":555,\"level\":100,\"voltage\":3.52},\"event\":3,\"time\":\"2020-10-01T12:45:48.207Z\"},\"time\":\"2020-10-01T12:45:42Z\",\"ueToken\":\"ALT12503DE04336CB2E3A4A113FCDE05DF05A6F\",\"uid\":\"WZuDMv5Je\"}")));

    }

}
