package org.traccar.geocoder;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeocoderTest {

    static {
        Locale.setDefault(Locale.US);
    }

    @Ignore
    @Test
    public void testGoogle() {
        Geocoder geocoder = new GoogleGeocoder(null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(31.776797, 35.211489, null);
        assertEquals("1 Ibn Shaprut St, Jerusalem, Jerusalem District, IL", address);
    }

    @Ignore
    @Test
    public void testNominatim() {
        Geocoder geocoder = new NominatimGeocoder(null, null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(40.7337807, -73.9974401, null);
        assertEquals("35 West 9th Street, NYC, New York, US", address);
    }

    @Ignore
    @Test
    public void testGisgraphy() {
        Geocoder geocoder = new GisgraphyGeocoder(null, 0, new AddressFormat());
        String address = geocoder.getAddress(48.8530000, 2.3400000, null);
        assertEquals("Rue du Jardinet, Paris, Île-de-France, FR", address);
    }

    @Ignore
    @Test
    public void testOpenCage() {
        Geocoder geocoder = new OpenCageGeocoder(
                "http://api.opencagedata.com/geocode/v1", "SECRET", null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("Charleston Road, California, US", address);
    }

    @Ignore
    @Test
    public void testGeocodeFarm() {
        Geocoder geocoder = new GeocodeFarmGeocoder(null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("Estrella Avenue, Arcadia, California, United States", address);
    }

    @Ignore
    @Test
    public void testGeocodeXyz() {
        Geocoder geocoder = new GeocodeXyzGeocoder(null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("605 ESTRELLA AVE, ARCADIA, California United States of America, US", address);
    }

    @Ignore
    @Test
    public void testBan() {
        Geocoder geocoder = new BanGeocoder(0, new AddressFormat("%f [%d], %c"));
        String address = geocoder.getAddress(48.8575, 2.2944, null);
        assertEquals("8 Avenue Gustave Eiffel 75007 Paris [75, Paris, Île-de-France], FR", address);
    }

    @Ignore
    @Test
    public void testHere() {
        Geocoder geocoder = new HereGeocoder(null, "", "", null, 0, new AddressFormat());
        String address = geocoder.getAddress(48.8575, 2.2944, null);
        assertEquals("6 Avenue Gustave Eiffel, Paris, Île-de-France, FRA", address);
    }

    @Ignore
    @Test
    public void testMapmyIndia() {
        Geocoder geocoder = new MapmyIndiaGeocoder("", "", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(28.6129602407977, 77.2294557094574, null);
        assertEquals("New Delhi, Delhi. 1 m from India Gate pin-110001 (India)", address);
    }

    @Ignore
    @Test
    public void testPositionStack() {
        Geocoder geocoder = new PositionStackGeocoder("", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(28.6129602407977, 77.2294557094574, null);
        assertEquals("India Gate, New Delhi, India", address);
    }

    @Ignore
    @Test
    public void testMapbox() {
        Geocoder geocoder = new MapboxGeocoder("", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("120 East 13th Street, New York, New York 10003, United States", address);
    }

    @Ignore
    @Test
    public void testMapTiler() {
        Geocoder geocoder = new MapTilerGeocoder("", 0, new AddressFormat());
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("East 13th Street, New York City, New York, United States", address);
    }

    @Ignore
    @Test
    public void testGeoapify() {
        Geocoder geocoder = new GeoapifyGeocoder("", null, 0, new AddressFormat());
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("114 East 13th Street, New York, New York, US", address);
    }

}
