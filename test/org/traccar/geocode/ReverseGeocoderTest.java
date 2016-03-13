package org.traccar.geocode;

import org.junit.Assert;
import org.junit.Test;

public class ReverseGeocoderTest {

    private boolean enable = false;

    @Test
    public void test() throws InterruptedException {
        if (enable) {
            testGoogle();
        }
    }

    private String address;

    private synchronized String waitAddress() {
        try {
            wait(5000);
            return address;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void setAddress(String address) {
        this.address = address;
        notifyAll();
    }

    public void testGoogle() throws InterruptedException {
        ReverseGeocoder reverseGeocoder = new GoogleReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 37.4217550, -122.0846330, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                setAddress(address);
            }
        });
        Assert.assertEquals("1600 Amphitheatre Pkwy, Mountain View, CA, US", waitAddress());
    }

    public void testNominatim() throws InterruptedException {
        ReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 40.7337807, -73.9974401, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                setAddress(address);
            }
        });
        Assert.assertEquals("35 West 9th Street, NYC, New York, US",  waitAddress());
    }

    public void testGisgraphy() throws InterruptedException {
        ReverseGeocoder reverseGeocoder = new GisgraphyReverseGeocoder();

        reverseGeocoder.getAddress(new AddressFormat(), 48.8530000, 2.3400000, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                setAddress(address);
            }
        });
        Assert.assertEquals("Rue du Jardinet, Paris, FR",  waitAddress());
    }

    public void testOpenCage() throws InterruptedException {
        ReverseGeocoder reverseGeocoder = new OpenCageReverseGeocoder(
                "http://api.opencagedata.com/geocode/v1", "SECRET", 0);

        reverseGeocoder.getAddress(new AddressFormat(), 34.116302, -118.051519, new ReverseGeocoder.ReverseGeocoderCallback() {
            @Override
            public void onResult(String address) {
                setAddress(address);
            }
        });
        Assert.assertEquals("Charleston Road, California, US",  waitAddress());
    }

}
