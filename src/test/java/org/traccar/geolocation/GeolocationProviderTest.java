package org.traccar.geolocation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.model.CellTower;
import org.traccar.model.Network;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class GeolocationProviderTest extends BaseTest {

    private final Client client = ClientBuilder.newClient();

    @Disabled
    @Test
    public void testMozilla() throws Exception {
        GoogleGeolocationProvider provider = new GoogleGeolocationProvider(client, null);

        Network network = new Network(CellTower.from(208, 1, 2, 1234567));

        provider.getLocation(network, new GeolocationProvider.LocationProviderCallback() {
            @Override
            public void onSuccess(double latitude, double longitude, double accuracy) {
                assertEquals(60.07254, latitude, 0.00001);
                assertEquals(30.30996, longitude, 0.00001);
            }

            @Override
            public void onFailure(Throwable e) {
                fail();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
