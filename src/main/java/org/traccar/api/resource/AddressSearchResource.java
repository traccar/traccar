package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.geocoder.ForwardGeocoder;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;
import org.traccar.model.Device;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import org.traccar.model.Group;
import org.traccar.helper.model.DeviceUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Resource for searching positions near a specific address.
 * This endpoint allows searching for vehicle positions within a specified radius of an address.
 * It uses a line segment distance calculation to better handle positions along streets.
 *
 * @author LECOMTE Benjamin
 */
@Path("search/address")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressSearchResource extends BaseResource {

    @Inject
    private ForwardGeocoder forwardGeocoder;

    /**
     * Calculates the distance between a point and a line segment.
     * This method is used to determine if a position is near a street segment.
     *
     * @param lat1 Latitude of the first point of the line segment
     * @param lon1 Longitude of the first point of the line segment
     * @param lat2 Latitude of the second point of the line segment
     * @param lon2 Longitude of the second point of the line segment
     * @param pointLat Latitude of the point to check
     * @param pointLon Longitude of the point to check
     * @return Distance in meters between the point and the line segment
     */
    private double distanceToLineSegment(double lat1, double lon1, double lat2, double lon2, double pointLat, double pointLon) {
        // Calculate distances between points
        double d1 = DistanceCalculator.distance(lat1, lon1, pointLat, pointLon);
        double d2 = DistanceCalculator.distance(lat2, lon2, pointLat, pointLon);
        double d3 = DistanceCalculator.distance(lat1, lon1, lat2, lon2);

        // If the segment is very short, use the distance to the closest point
        if (d3 < 0.0001) {
            return Math.min(d1, d2);
        }

        // Calculate the projection of the point onto the line segment
        double t = ((pointLat - lat1) * (lat2 - lat1) + (pointLon - lon1) * (lon2 - lon1)) /
                  ((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1));

        // If the projection is outside the segment, use the distance to the closest endpoint
        if (t < 0) return d1;
        if (t > 1) return d2;

        // Calculate the projected point
        double projLat = lat1 + t * (lat2 - lat1);
        double projLon = lon1 + t * (lon2 - lon1);

        // Return the distance to the projected point
        return DistanceCalculator.distance(projLat, projLon, pointLat, pointLon);
    }

    /**
     * Searches for positions near a specific address within a given time range and radius.
     *
     * @param address The address to search around
     * @param from Start date of the search period
     * @param to End date of the search period
     * @param radius Search radius in meters (default: 100)
     * @param deviceIds List of device IDs to filter by
     * @param groupIds List of group IDs to filter by
     * @return Response containing the found positions grouped by device
     * @throws Exception if an error occurs during the search
     */
    @GET
    public Response search(
            @QueryParam("address") String address,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("radius") Double radius,
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds) throws Exception {

        if (address == null || from == null || to == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        double searchRadius = radius != null ? radius : 100.0;
        var coordinates = forwardGeocoder.getCoordinates(address);
        if (coordinates == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Get all device IDs to filter (from groups if needed)
        List<Long> allDeviceIds = new ArrayList<>();
        if (groupIds != null && !groupIds.isEmpty()) {
            var accessibleDevices = DeviceUtil.getAccessibleDevices(storage, getUserId(), Collections.emptyList(), groupIds);
            for (var device : accessibleDevices) {
                allDeviceIds.add(device.getId());
            }
        }
        if (deviceIds != null && !deviceIds.isEmpty()) {
            allDeviceIds.addAll(deviceIds);
        }
        if (allDeviceIds.isEmpty()) {
            var accessibleDevices = DeviceUtil.getAccessibleDevices(storage, getUserId(), Collections.emptyList(), Collections.emptyList());
            for (var device : accessibleDevices) {
                allDeviceIds.add(device.getId());
            }
        }
        boolean filterByDevice = true;

        List<Position> positions = storage.getObjects(Position.class, new Request(
                new Columns.All(),
                new Condition.Between("fixTime", "from", from, "to", to)));

        // Filter positions by radius and device if needed
        var results = new ArrayList<Position>();
        for (Position position : positions) {
            if (filterByDevice && !allDeviceIds.contains(position.getDeviceId())) {
                continue;
            }

            // Calculate distance to the nearest street segment
            double distance = distanceToLineSegment(
                coordinates.getLatitude(), coordinates.getLongitude(),
                coordinates.getLatitude() + 0.0001, coordinates.getLongitude() + 0.0001,
                position.getLatitude(), position.getLongitude()
            );

            if (distance <= searchRadius) {
                results.add(position);
            }
        }

        // Group positions by deviceId
        Map<Long, List<Position>> positionsByDevice = results.stream()
                .collect(Collectors.groupingBy(Position::getDeviceId));

        // Build enriched response
        List<Map<String, Object>> response = new ArrayList<>();
        for (Map.Entry<Long, List<Position>> entry : positionsByDevice.entrySet()) {
            long deviceId = entry.getKey();
            List<Position> route = entry.getValue();
            Device device = storage.getObject(Device.class, new Request(
                    new Columns.Include("name"),
                    new Condition.Equals("id", deviceId)));
            Map<String, Object> item = new HashMap<>();
            item.put("deviceId", deviceId);
            item.put("deviceName", device != null ? device.getName() : "");
            item.put("route", route);
            response.add(item);
        }

        return Response.ok(response).build();
    }
} 