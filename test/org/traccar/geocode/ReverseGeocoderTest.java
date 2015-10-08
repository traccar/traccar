package org.traccar.geocode;

import org.junit.Assert;
import org.junit.Test;

public class ReverseGeocoderTest {

    private boolean enable = false;

    @Test
    public void test() {
        if (enable) {
            testGoogle();
            testNominatim();
            testGisgraphy();
        }

    }

    public void testGoogle() {

        ReverseGeocoder reverseGeocoder = new GoogleReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 37.4217550, -122.0846330, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                Assert.assertEquals("1600 Amphitheatre Pkwy, Mountain View, CA, US", address);
            }
        });
    }

    public void testNominatim() {

        ReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 40.7337807, -73.9974401, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                Assert.assertEquals("35 West 9th Street, NYC, New York, US", address);
            }
        });
    }

    public void testGisgraphy() {

        ReverseGeocoder reverseGeocoder = new GisgraphyReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 48.8530000, 2.3400000, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                Assert.assertEquals("Rue du Jardinet, Paris, FR", address);
            }
        });
    }

}
