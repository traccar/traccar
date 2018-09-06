package org.traccar.geocoder;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

        assertEquals("1 Ibn Shaprut St, Jerusalem, Jerusalem District, IL", waitAddress());
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

        assertEquals("35 West 9th Street, NYC, New York, US", waitAddress());
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

        assertEquals("Rue du Jardinet, Paris, Île-de-France, FR", waitAddress());
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

        assertEquals("Charleston Road, California, US", waitAddress());
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

        assertEquals("Estrella Avenue, Arcadia, California, United States", waitAddress());
    }

    public void testGeocodeXyz() throws InterruptedException {
        Geocoder geocoder = new GeocodeXyzGeocoder(null, 0, new AddressFormat());

        geocoder.getAddress(34.116302, -118.051519, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });

        assertEquals("605 ESTRELLA AVE, ARCADIA, California United States of America, US", waitAddress());
    }

    @Ignore
    @Test
    public void testBan() throws InterruptedException {
        Geocoder geocoder = new BanGeocoder(0, new AddressFormat("%f [%d], %c"));

        geocoder.getAddress(48.8575, 2.2944, new Geocoder.ReverseGeocoderCallback() {
            @Override
            public void onSuccess(String address) {
                setAddress(address);
            }

            @Override
            public void onFailure(Throwable e) {
            }
        });

        assertEquals("8 Avenue Gustave Eiffel 75007 Paris [75, Paris, Île-de-France], FR", waitAddress());
    }

}
