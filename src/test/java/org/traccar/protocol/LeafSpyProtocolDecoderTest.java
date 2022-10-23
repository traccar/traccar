package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class LeafSpyProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new LeafSpyProtocolDecoder(null));

        verifyNull(decoder, request(
                "/?Lat=60.0&Long=30.0"));

        verifyPosition(decoder, request(
                "/?user=driver&pass=123456&DevBat=80&Gids=200&Lat=60.0&Long=30.0&Elv=5&Seq=50&Trip=1&Odo=10000&SOC=99.99&AHr=55.00&BatTemp=15.2&Amb=12.0&Wpr=12&PlugState=0&ChrgMode=0&ChrgPwr=0&VIN=ZE0-000000&PwrSw=1&Tunits=C&RPM=1000"));

    }

}
