package org.traccar.geocoder;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GeocoderTest {

    @Ignore
    @Test
    public void test() throws InterruptedException {
        Locale.setDefault(Locale.US);
        testGoogle();
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
        Geocoder geocoder = new GoogleGeocoder(null, null, 0, new AddressFormat());

        geocoder.getAddress(31.776797, 35.211489, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(final Throwable e) {
            }
        });
        Assert.assertEquals("1 Ibn Shaprut St, Jerusalem, Jerusalem District, IL", waitAddress());

        Assert.assertEquals("1 Ibn Shaprut St, Jerusalem, Jerusalem District, IL",
                geocoder.getAddress(31.776797, 35.211489, null));
    }

    public void testNominatim() throws InterruptedException {
        Geocoder geocoder = new NominatimGeocoder(null, null, null, 0, new AddressFormat());

        geocoder.getAddress(40.7337807, -73.9974401, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("35 West 9th Street, NYC, New York, US", waitAddress());

        Assert.assertEquals("35 West 9th Street, NYC, New York, US",
                geocoder.getAddress(40.7337807, -73.9974401, null));
    }

    public void testGisgraphy() throws InterruptedException {
        Geocoder geocoder = new GisgraphyGeocoder(new AddressFormat());

        geocoder.getAddress(48.8530000, 2.3400000, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Rue du Jardinet, Paris, FR", waitAddress());

        Assert.assertEquals("Rue du Jardinet, Paris, FR", geocoder.getAddress(48.8530000, 2.3400000, null));
    }

    public void testOpenCage() throws InterruptedException {
        Geocoder geocoder = new OpenCageGeocoder(
                "http://api.opencagedata.com/geocode/v1", "SECRET", 0, new AddressFormat());

        geocoder.getAddress(34.116302, -118.051519, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Charleston Road, California, US", waitAddress());

        Assert.assertEquals("Charleston Road, California, US", geocoder.getAddress(34.116302, -118.051519, null));
    }

    public void testGeocodeFarm() throws InterruptedException {
        Geocoder geocoder = new GeocodeFarmGeocoder(null, null, 0, new AddressFormat());

        geocoder.getAddress(34.116302, -118.051519, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });
        Assert.assertEquals("Estrella Avenue, Arcadia, California, United States", waitAddress());

        Assert.assertEquals("Estrella Avenue, Arcadia, California, United States",
                geocoder.getAddress(34.116302, -118.051519, null));
    }

}
