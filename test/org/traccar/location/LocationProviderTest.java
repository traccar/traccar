package org.traccar.location;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.util.HashMap;
import java.util.Map;

public class LocationProviderTest extends BaseTest {

    private boolean enable = false;

    @Test
    public void test() {
        if (enable) {
            testMozilla();
        }
    }

    public void testMozilla() {
        MozillaLocationProvider locationProvider = new MozillaLocationProvider();

        Network network = new Network(CellTower.from(260, 2, 10250, 26511));

        locationProvider.getLocation(network, new LocationProvider.LocationProviderCallback() {
            @Override
            public void onSuccess(double latitude, double longitude, double accuracy) {
                Assert.assertEquals(60.07254, latitude, 0.00001);
                Assert.assertEquals(30.30996, longitude, 0.00001);
            }

            @Override
            public void onFailure() {
                Assert.fail();
            }
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
