package org.traccar.location;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Position;

import java.util.HashMap;
import java.util.Map;

public class LocationProviderTest {

    private boolean enable = false;

    @Test
    public void test() {
        if (enable) {
            testOpenCellId();
        }
    }

    public void testOpenCellId() {
        OpenCellIdLocationProvider locationProvider = new OpenCellIdLocationProvider("fake");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Position.KEY_MCC, 260);
        attributes.put(Position.KEY_MNC, 2);
        attributes.put(Position.KEY_LAC, 10250);
        attributes.put(Position.KEY_CID, 26511);

        locationProvider.getLocation(attributes, new LocationProvider.LocationProviderCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                Assert.assertEquals(60.07254, latitude, 0.00001);
                Assert.assertEquals(30.30996, longitude, 0.00001);
            }

            @Override
            public void onFailure() {
                Assert.fail();
            }
        });
    }

}
