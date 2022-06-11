package org.traccar.speedlimit;

import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OverpassSpeedLimitProviderTest {

    private final Client client = ClientBuilder.newClient();

    @Ignore
    @Test
    public void testOverpass() throws Exception {
        SpeedLimitProvider provider = new OverpassSpeedLimitProvider(client, "http://8.8.8.8/api/interpreter");

        provider.getSpeedLimit(34.74767, -82.48098, new SpeedLimitProvider.SpeedLimitProviderCallback() {
            @Override
            public void onSuccess(double speedLimit) {
                assertEquals(52.1, speedLimit, 0.1);
            }

            @Override
            public void onFailure(Throwable e) {
                fail();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
