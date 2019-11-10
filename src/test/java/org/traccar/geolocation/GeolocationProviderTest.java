package org.traccar.geolocation;

import org.junit.Ignore;
import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.CellTower;
import org.traccar.model.Network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GeolocationProviderTest extends BaseTest {

    @Ignore
    @Test
    public void test() throws Exception {
        testLocationProvider();
    }

    public void testLocationProvider() throws Exception {
        MozillaGeolocationProvider provider = new MozillaGeolocationProvider(null);

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
