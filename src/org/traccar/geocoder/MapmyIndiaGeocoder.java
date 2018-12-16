package org.traccar.geocoder;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class MapmyIndiaGeocoder extends JsonGeocoder {

    public MapmyIndiaGeocoder(String url, String key, int cacheSize, AddressFormat addressFormat) {
        super(url + "/" + key + "/rev_geocode?lat=%f&lng=%f", cacheSize, addressFormat);
    }

    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray results = json.getJsonArray("results");

        if (!results.isEmpty()) {
            Address address = new Address();

            JsonObject result = (JsonObject) results.get(0);

            if (result.containsKey("formatted_address")) {
                address.setFormattedAddress(result.getString("formatted_address"));
            }

            if (result.containsKey("house_number") && (result.getString("house_number") != "")) {
                address.setHouse(result.getString("house_number"));
            } else if (result.containsKey("house_name") && (result.getString("house_name") != "")) {
                address.setHouse(result.getString("house_name"));
            }

            if (result.containsKey("street")) {
                address.setStreet(result.getString("street"));
            }

            if (result.containsKey("locality") && (result.getString("locality") != "")) {
                address.setSuburb(result.getString("locality"));
            } else if (result.containsKey("sublocality") && (result.getString("sublocality") != "")) {
                address.setSuburb(result.getString("sublocality"));
            } else if (result.containsKey("subsublocality") && (result.getString("subsublocality") != "")) {
                address.setSuburb(result.getString("subsublocality"));
            }

            if (result.containsKey("city") && (result.getString("city") != "")) {
                address.setSettlement(result.getString("city"));
            } else if (result.containsKey("village") && (result.getString("village") != "")) {
                address.setSettlement(result.getString("village"));
            }

            if (result.containsKey("district")) {
                address.setDistrict(result.getString("district"));
            } else if (result.containsKey("subDistrict")) {
                address.setDistrict(result.getString("subDistrict"));
            }

            if (result.containsKey("state")) {
                address.setState(result.getString("state"));
            }

            if (result.containsKey("pincode")) {
                address.setPostcode(result.getString("pincode"));
            }

            return address;
        }
        return null;
    }
}
