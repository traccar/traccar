package org.traccar.geolocation;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.CellTower;
import org.traccar.model.Network;

public class GeolocationProviderTest extends BaseTest {

    private boolean enable = false;

    @Test
    public void test() throws Exception {
        if (enable) {
            testGoogleLocationProvider();
        }
    }

    public void testGoogleLocationProvider() throws Exception {
        GoogleGeolocationProvider locationProvider = new GoogleGeolocationProvider("KEY");

        Network network = new Network(CellTower.from(260, 2, 10250, 26511));

        locationProvider.getLocation(network, new GeolocationProvider.LocationProviderCallback() {
            @Override
            public void onSuccess(double latitude, double longitude, double accuracy) {
                Assert.assertEquals(60.07254, latitude, 0.00001);
                Assert.assertEquals(30.30996, longitude, 0.00001);
            }

            @Override
            public void onFailure(Throwable e) {
                Assert.fail();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
