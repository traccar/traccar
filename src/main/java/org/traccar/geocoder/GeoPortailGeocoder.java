package org.traccar.geocoder;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Created by goebel on 4/17/19.
 */
public class GeoPortailGeocoder extends JsonGeocoder {

    public GeoPortailGeocoder(String url, int cacheSize, AddressFormat addressFormat) {
        super("https://api.geoportail.lu/geocoder/reverseGeocode?lat=%f&lon=%f", cacheSize, new AddressFormat("%h %r, %p, %t, %s, %c"));
    }


    @Override
    public Address parseAddress(JsonObject json) {
        JsonArray result = json.getJsonArray("results");

        if (result != null && !result.isEmpty()) {
            JsonObject location = result.getJsonObject(0).getJsonObject("AddressDetails");
            Address address = new Address();

            address.setCountry("LU");
            if (location.containsKey("zip")) {
                address.setPostcode(location.getString("zip"));
            }
            if (location.containsKey("context")) {
                address.setDistrict(location.getString("context"));
            }
            if (location.containsKey("street")) {
                address.setStreet(location.getString("street"));
            }
            if (location.containsKey("locality")) {
                address.setSettlement(location.getString("locality"));
            }
            if (location.containsKey("postnumber")) {
                address.setHouse(location.getString("postnumber"));
            }
            if (result.getJsonObject(0).containsKey("address")) {
                address.setFormattedAddress(result.getJsonObject(0).getString("address"));
            }

            return address;
        }

        return null;
    }




}
