package org.traccar.geocoder;

import org.junit.Assert;
import org.junit.Test;

public class GeocoderTest {

    private boolean enable = false;

    @Test
    public void test() throws InterruptedException {
        if (enable) {
            testGeocodeFarm();
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
        Geocoder geocoder = new GoogleGeocoder();

        geocoder.getAddress(new AddressFormat(), 37.4217550, -122.0846330, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("1600 Amphitheatre Pkwy, Mountain View, CA, US", waitAddress());
    }

    public void testNominatim() throws InterruptedException {
        Geocoder geocoder = new NominatimGeocoder();

        geocoder.getAddress(new AddressFormat(), 40.7337807, -73.9974401, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("35 West 9th Street, NYC, New York, US",  waitAddress());
    }

    public void testGisgraphy() throws InterruptedException {
        Geocoder geocoder = new GisgraphyGeocoder();

        geocoder.getAddress(new AddressFormat(), 48.8530000, 2.3400000, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Rue du Jardinet, Paris, FR",  waitAddress());
    }

    public void testOpenCage() throws InterruptedException {
        Geocoder geocoder = new OpenCageGeocoder(
                "http://api.opencagedata.com/geocode/v1", "SECRET", 0);

        geocoder.getAddress(new AddressFormat(), 34.116302, -118.051519, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Charleston Road, California, US",  waitAddress());
    }

    public void testGeocodeFarm() throws InterruptedException {
        Geocoder geocoder = new GeocodeFarmGeocoder(0);

        geocoder.getAddress(new AddressFormat(), 34.116302, -118.051519, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Estrella Avenue, Arcadia, California, United States",  waitAddress());
    }

}
