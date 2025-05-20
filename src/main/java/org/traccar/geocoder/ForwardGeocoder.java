package org.traccar.geocoder;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

public class ForwardGeocoder {
    private final Client client;
    private final String apiKey;

    public ForwardGeocoder(Client client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    public Coordinates getCoordinates(String address) {
        WebTarget target = client.target("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address)
                .queryParam("key", apiKey);

        JsonObject response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get(JsonObject.class);

        if (response.getString("status").equals("OK")) {
            JsonObject location = response.getJsonArray("results")
                    .getJsonObject(0)
                    .getJsonObject("geometry")
                    .getJsonObject("location");

            return new Coordinates(
                    location.getJsonNumber("lat").doubleValue(),
                    location.getJsonNumber("lng").doubleValue()
            );
        }

        return null;
    }

    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
} 