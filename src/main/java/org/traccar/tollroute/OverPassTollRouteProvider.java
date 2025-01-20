package org.traccar.tollroute;

import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.InvocationCallback;



public class OverPassTollRouteProvider implements TollRouteProvider {
    private final Client client;
    private final String url;
    private final int accuracy;

    public OverPassTollRouteProvider(Config config, Client client, String url) {
        this.client = client;
        //! got the url from the config , and set it using the base url , hope this works :)
        final String Baseurl = config.getString(Keys.TOLL_ROUTE_URL, url);
        this.accuracy = config.getInteger(Keys.TOLL_ROUTE_ACCURACY);
        this.url = Baseurl + "?data=[out:json];way[toll](around:" + accuracy + ",%f,%f);out%%20tags;";
    }

    @Override
    public void getTollRoute(double latitude, double longitude, TollRouteProviderCallback callback) {
        String formattedUrl = String.format(url, latitude, longitude);
        AsyncInvoker invoker = client.target(formattedUrl).request().async();
        invoker.get(new InvocationCallback<JsonObject>() {
            @Override
            public void completed(JsonObject json) {
                JsonArray elements = json.getJsonArray("elements");
                if (!elements.isEmpty()) {
                    // we get the json object with the toll cost
                    // it kinda looks like this : 
                    //     "tags": {
                    //     "embankment": "yes",
                    //     "highway": "trunk",
                    //     "lanes": "3",
                    //     "name": "Old Pune–Mumbai Highway",
                    //     "name:mr": "जुना पुणे-मुंबई महामार्ग",
                    //     "oneway": "yes",
                    //     "ref": "NH60;NH65",
                    //     "toll": "no"
                    //   }
                    // we need the toll , ref and name , in that order
                    JsonObject tags = elements.getJsonObject(0).getJsonObject("tags");
                    String toll = tags.getString("toll", "no");
                    String ref = tags.getString("ref", "");
                    String name = tags.getString("name", "");
                    if (tags.containsKey("toll") && tags.getString("toll").equals("yes")) {
                        // Default toll cost if found
                        callback.onSuccess(new TollData(toll, ref, name));
                    } else {
                        callback.onFailure(new RuntimeException("No toll data found with specific parameters"));
                    }
                } else {
                    callback.onFailure(new RuntimeException("No toll data found"));
                }
            }

            @Override
            public void failed(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

}