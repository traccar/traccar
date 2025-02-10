package org.traccar.tolltoute;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.speedlimit.OverpassSpeedLimitProvider;
import org.traccar.speedlimit.SpeedLimitProvider;
import org.traccar.tollroute.OverPassTollRouteProvider;
import org.traccar.tollroute.TollData;
import org.traccar.tollroute.TollRouteProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OverpassTollRouteProviderTest {

    private final Client client = ClientBuilder.newClient();

    @Disabled
    @Test
    public void testOverpass() throws Exception {
        var config = new Config();
        TollRouteProvider provider = new OverPassTollRouteProvider(config, client, "https://overpass.private.coffee/api/interpreter");

        provider.getTollRoute(43.34419,-79.83133, new TollRouteProvider.TollRouteProviderCallback() {

            @Override
            public void onSuccess(TollData tollCost) {
                System.out.println(tollCost.getToll());
                System.out.println(tollCost.getName());
                System.out.println(tollCost.getRef());

            }

            @Override
            public void onFailure(Throwable e) {
                fail();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
