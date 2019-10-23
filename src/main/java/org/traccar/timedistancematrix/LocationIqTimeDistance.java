package org.traccar.timedistancematrix;

import org.traccar.Context;

import javax.json.JsonObject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Invocation;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class LocationIqTimeDistance extends JsonTimeDistance {
    public LocationIqTimeDistance(String url, String key) {
        super(url, key);
    }

    @Override
    public JsonObject getTimeDistanceResponse(String url, String key, TimeDistanceRequest timeDistanceRequest)
            throws ClientErrorException {
        if (url == null) {
            url = "https://us1.locationiq.com/v1/matrix/driving/";
        }
        String metrics = String.join(",", timeDistanceRequest.getMetrics());

        StringJoiner locationsStringJoiner = new StringJoiner(";");
        for (List<Double> coordinates : timeDistanceRequest.getLocations()) {
            StringJoiner locationStringJoiner = new StringJoiner(",");
            for (Double point : coordinates) {
                locationStringJoiner.add(Objects.toString(point));
            }
            locationsStringJoiner.add(locationStringJoiner.toString());
        }
        String locations = locationsStringJoiner.toString();

        StringJoiner sourcesJoiner = new StringJoiner(";");
        for (int source : timeDistanceRequest.getSources()) {
            sourcesJoiner.add(Objects.toString(source));
        }
        String sources = sourcesJoiner.toString();

        String destinations = Objects.toString(timeDistanceRequest.getDestinations().get(0));

        String finalUrl = String.format(
                "%s%s?destinations=%s&sources=%s&annotations=%s&key=%s",
                url,
                locations,
                destinations, sources,
                metrics,
                key);

        Invocation.Builder request = Context.getClient().target(finalUrl)
                .request();

        return request.get(JsonObject.class);
    }
}
