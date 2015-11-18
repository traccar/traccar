package org.traccar.location;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.geocode.AddressFormat;
import org.traccar.geocode.GisgraphyReverseGeocoder;
import org.traccar.geocode.GoogleReverseGeocoder;
import org.traccar.geocode.NominatimReverseGeocoder;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.model.Event;

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
        OpenCellIdLocationProvider locationProvider = new OpenCellIdLocationProvider();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Event.KEY_MCC, 250);
        attributes.put(Event.KEY_MNC, 2);
        attributes.put(Event.KEY_LAC, 4711);
        attributes.put(Event.KEY_CID, 7989334);

        locationProvider.getLocation(attributes, new LocationProvider.LocationProviderCallback() {
            @Override
            public void onResult(double latitude, double longitude) {
                Assert.assertEquals(60.07254, latitude, 0.00001);
                Assert.assertEquals(30.30996, longitude, 0.00001);
            }
        });
    }

}
