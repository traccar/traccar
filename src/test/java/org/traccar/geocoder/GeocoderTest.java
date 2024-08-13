package org.traccar.geocoder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeocoderTest {

    static {
        Locale.setDefault(Locale.US);
    }

    private final Client client = ClientBuilder.newClient();

    @Disabled
    @Test
    public void testGoogle() {
        Geocoder geocoder = new GoogleGeocoder(client, null, null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(31.776797, 35.211489, null);
        assertEquals("1 Ibn Shaprut St, Jerusalem, Jerusalem District, IL", address);
    }

    @Disabled
    @Test
    public void testNominatim() {
        Geocoder geocoder = new NominatimGeocoder(client, null, null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(40.7337807, -73.9974401, null);
        assertEquals("35 West 9th Street, NYC, New York, US", address);
    }

    @Disabled
    @Test
    public void testGisgraphy() {
        Geocoder geocoder = new GisgraphyGeocoder(client, null, 0, new AddressFormat());
        String address = geocoder.getAddress(48.8530000, 2.3400000, null);
        assertEquals("Rue du Jardinet, Paris, Île-de-France, FR", address);
    }

    @Disabled
    @Test
    public void testOpenCage() {
        Geocoder geocoder = new OpenCageGeocoder(
                client, "http://api.opencagedata.com/geocode/v1", "SECRET", null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("Charleston Road, California, US", address);
    }

    @Disabled
    @Test
    public void testGeocodeFarm() {
        Geocoder geocoder = new GeocodeFarmGeocoder(client, null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("Estrella Avenue, Arcadia, California, United States", address);
    }

    @Disabled
    @Test
    public void testGeocodeXyz() {
        Geocoder geocoder = new GeocodeXyzGeocoder(client, null, 0, new AddressFormat());
        String address = geocoder.getAddress(34.116302, -118.051519, null);
        assertEquals("605 ESTRELLA AVE, ARCADIA, California United States of America, US", address);
    }

    @Disabled
    @Test
    public void testBan() {
        Geocoder geocoder = new BanGeocoder(client, 0, new AddressFormat());
        String address = geocoder.getAddress(48.8575, 2.2944, null);
        assertEquals("8 Avenue Gustave Eiffel, Paris, FR", address);
    }

    @Disabled
    @Test
    public void testHere() {
        Geocoder geocoder = new HereGeocoder(client, null, "aDc9qgsCpRbO9ioJIIAXzF6JYU7w8H5O260e9hsGrms", null, 0, new AddressFormat());
        String address = geocoder.getAddress(48.8575, 2.2944, null);
        assertEquals("1 Tour Eiffel, Paris, Île-de-France, FRA", address);
    }

    @Disabled
    @Test
    public void testMapmyIndia() {
        Geocoder geocoder = new MapmyIndiaGeocoder(client, "", "", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(28.6129602407977, 77.2294557094574, null);
        assertEquals("New Delhi, Delhi. 1 m from India Gate pin-110001 (India)", address);
    }

    @Disabled
    @Test
    public void testPositionStack() {
        Geocoder geocoder = new PositionStackGeocoder(client, "", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(28.6129602407977, 77.2294557094574, null);
        assertEquals("India Gate, New Delhi, India", address);
    }

    @Disabled
    @Test
    public void testMapbox() {
        Geocoder geocoder = new MapboxGeocoder(client, "", 0, new AddressFormat("%f"));
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("120 East 13th Street, New York, New York 10003, United States", address);
    }

    @Disabled
    @Test
    public void testMapTiler() {
        Geocoder geocoder = new MapTilerGeocoder(client, "", 0, new AddressFormat());
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("East 13th Street, New York City, New York, United States", address);
    }

    @Disabled
    @Test
    public void testGeoapify() {
        Geocoder geocoder = new GeoapifyGeocoder(client, "", null, 0, new AddressFormat());
        String address = geocoder.getAddress(40.733, -73.989, null);
        assertEquals("114 East 13th Street, New York, New York, US", address);
    }

    @Disabled
    @Test
    public void testGeocodeJSON() {
        Geocoder geocoder = new GeocodeJsonGeocoder(client, null, null, null, 0, new AddressFormat());
        String address = geocoder.getAddress(40.7337807, -73.9974401, null);
        assertEquals("35 West 9th Street, New York, New York, US", address);
    }
}
