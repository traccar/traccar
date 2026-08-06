/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;
import org.traccar.helper.DateUtil;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.RouteReportProvider;
import org.traccar.reports.SummaryReportProvider;
import org.traccar.reports.TripsReportProvider;
import org.traccar.reports.model.SummaryReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Singleton
public class McpServerHolder implements AutoCloseable {

    public static final String PATH = "/api/mcp";

    private static final McpSchema.ToolAnnotations READ_ONLY_ANNOTATIONS = new McpSchema.ToolAnnotations(
            null, true, false, true, false, null);

    private final Storage storage;
    private final Provider<PermissionsService> permissionsService;
    private final Geocoder geocoder;
    private final boolean geocodeOnRequest;
    private final Provider<SummaryReportProvider> summaryReportProvider;
    private final Provider<TripsReportProvider> tripsReportProvider;
    private final Provider<RouteReportProvider> routeReportProvider;

    private final HttpServletStreamableServerTransportProvider transport;
    private final McpAsyncServer server;

    @Inject
    public McpServerHolder(
            ObjectMapper objectMapper, Storage storage, Provider<PermissionsService> permissionsService,
            Config config, @Nullable Geocoder geocoder,
            Provider<SummaryReportProvider> summaryReportProvider,
            Provider<TripsReportProvider> tripsReportProvider,
            Provider<RouteReportProvider> routeReportProvider) {

        this.storage = storage;
        this.permissionsService = permissionsService;
        this.geocoder = geocoder;
        this.summaryReportProvider = summaryReportProvider;
        this.tripsReportProvider = tripsReportProvider;
        this.routeReportProvider = routeReportProvider;
        geocodeOnRequest = config.getBoolean(Keys.GEOCODER_ON_REQUEST);

        transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(PATH)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .contextExtractor(this::extractTransportContext)
                .build();

        var capabilities = McpSchema.ServerCapabilities.builder()
                .tools(true)
                .resources(false, true)
                .prompts(true)
                .build();

        server = McpServer.async(transport)
                .serverInfo("traccar-mcp", "1.0.0")
                .capabilities(capabilities)
                .tools(
                        createVersionTool(), createDevicePositionTool(), createDeviceListTool(),
                        createDeviceSummaryTool(), createDeviceTripsTool(), createDeviceRouteTool())
                .build();
    }

    private McpTransportContext extractTransportContext(HttpServletRequest request) {
        var contextData = new HashMap<String, Object>();
        Object userId = request.getAttribute(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId != null) {
            contextData.put(McpAuthFilter.ATTRIBUTE_USER_ID, userId);
        }
        if (contextData.isEmpty()) {
            return McpTransportContext.EMPTY;
        }
        return McpTransportContext.create(contextData);
    }

    private Map<String, Object> schemaProperty(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private McpSchema.JsonSchema deviceRangeInputSchema(Map<String, Object> extraProperties) {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("deviceId", schemaProperty("number", "Device id, see device-list for available ids"));
        properties.put("from", schemaProperty("string", "Start of the time range, ISO-8601, e.g. "
                + "2024-01-01T00:00:00Z"));
        properties.put("to", schemaProperty("string", "End of the time range, ISO-8601, e.g. "
                + "2024-01-02T00:00:00Z"));
        properties.putAll(extraProperties);
        return new McpSchema.JsonSchema(
                "object", properties, List.of("deviceId", "from", "to"), null, null, null);
    }

    private McpServerFeatures.AsyncToolSpecification createVersionTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object", Map.of(), null, null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("traccar-version")
                .title("Returns server version name")
                .description("Returns the Traccar server version string.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> {
                    String version = getClass().getPackage().getImplementationVersion();
                    return Mono.just(McpSchema.CallToolResult.builder()
                            .addTextContent(version != null ? version : "Unknown")
                            .build());
                })
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDevicePositionTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("deviceId", schemaProperty("number", "Device id, see device-list for available ids")),
                List.of("deviceId"),
                null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("device-position")
                .title("Returns latest device position with address and other parameters")
                .description(
                        "Returns the latest known position for a single device, including address, "
                                + "coordinates and raw protocol attributes. Speed is reported in knots.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDevicePosition)
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDeviceListTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "name", schemaProperty("string", "Case-insensitive substring filter on device name"),
                        "limit", schemaProperty("integer", "Maximum number of devices to return, default 200")),
                null, null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("device-list")
                .title("Lists accessible devices, optionally filtered by name")
                .description(
                        "Lists devices accessible to the current user with id, name, uniqueId, status "
                                + "and lastUpdate. Use the returned id as deviceId in other tools.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDeviceList)
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDeviceSummaryTool() {

        var inputSchema = deviceRangeInputSchema(Map.of(
                "daily", schemaProperty("boolean",
                        "Return one summary per day instead of one for the whole range, default false"),
                "limit", schemaProperty("integer",
                        "Maximum summaries to return, relevant when daily is true, default 60, clamped to "
                                + "1-1000")));

        var toolSchema = McpSchema.Tool.builder()
                .name("device-summary")
                .title("Returns aggregate distance/duration for a device over a time range")
                .description(
                        "Returns a single summary (or one per day if daily is true) with total distance, "
                                + "average/max speed, fuel, odometer and engine hours. Use this when you need "
                                + "totals for a long range rather than a leg-by-leg breakdown. When daily is "
                                + "true the result count is bounded by limit.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDeviceSummary)
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDeviceTripsTool() {

        var inputSchema = deviceRangeInputSchema(Map.of(
                "limit", schemaProperty("integer", "Maximum trips to return, default 100, clamped to 1-1000")));

        var toolSchema = McpSchema.Tool.builder()
                .name("device-trips")
                .title("Returns detected trips for a device within a time range")
                .description(
                        "Returns detected trips (motion-based segments) for a device within a time range, "
                                + "each with start/end time, coordinates, address, distance and speed. Use "
                                + "device-summary instead if you only need totals for a long range rather than "
                                + "a leg-by-leg breakdown.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDeviceTrips)
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDeviceRouteTool() {

        var inputSchema = deviceRangeInputSchema(Map.of(
                "limit", schemaProperty("integer",
                        "Maximum positions to return after filtering, default 200, clamped to 1-2000"),
                "dedup", schemaProperty("boolean",
                        "Collapse consecutive stationary positions to first/last of each run, default true. "
                                + "Set false for raw fidelity within the same limit, e.g. diagnosing a flaky "
                                + "tracker or verifying a reported distance.")));

        var toolSchema = McpSchema.Tool.builder()
                .name("device-route")
                .title("Returns recorded positions for a device within a time range")
                .description(
                        "Returns recorded positions for a device within a time range, ordered by time. "
                                + "Consecutive stationary positions (motion attribute false) are collapsed to "
                                + "just the first and last of each stationary run unless dedup is false, and "
                                + "the result is downsampled to at most limit points while always keeping the "
                                + "first and last point. Speed is reported in knots. The response includes "
                                + "rawCount (positions returned by the server before this tool's own filtering, "
                                + "itself capped by the server's report.maxPositions setting for very large "
                                + "ranges) and returnedCount so filtering is visible to the caller. For bulk or "
                                + "full-fidelity export, use the Reports page GPX/KML/XLSX export instead.")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDeviceRoute)
                .build();
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
    }

    private record DeviceRange(long deviceId, Date from, Date to) {
    }

    private DeviceRange parseDeviceRange(McpSchema.CallToolRequest request) {
        Object deviceIdValue = request.arguments().get("deviceId");
        if (!(deviceIdValue instanceof Number deviceIdNumber)) {
            throw new IllegalArgumentException("deviceId argument is required");
        }
        Object fromValue = request.arguments().get("from");
        Object toValue = request.arguments().get("to");
        if (!(fromValue instanceof String fromText) || !(toValue instanceof String toText)) {
            throw new IllegalArgumentException("from and to arguments are required");
        }
        try {
            return new DeviceRange(
                    deviceIdNumber.longValue(), DateUtil.parseDate(fromText), DateUtil.parseDate(toText));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid from/to date-time: expected ISO-8601, e.g. 2024-01-01T00:00:00Z");
        }
    }

    private int limitArgument(McpSchema.CallToolRequest request, int defaultValue, int maximum) {
        Object limitValue = request.arguments().get("limit");
        int limit = limitValue instanceof Number number ? number.intValue() : defaultValue;
        return Math.max(1, Math.min(limit, maximum));
    }

    private Mono<McpSchema.CallToolResult> getDevicePosition(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        Object deviceIdValue = request.arguments().get("deviceId");
        if (!(deviceIdValue instanceof Number deviceIdNumber)) {
            return Mono.just(errorResult("deviceId argument is required"));
        }

        long deviceId = deviceIdNumber.longValue();

        try {
            permissionsService.get().checkPermission(Device.class, userId, deviceId);

            Position position = storage.getObject(Position.class, new Request(
                    new Columns.All(), new Condition.LatestPositions(deviceId)));

            if (position == null) {
                return Mono.just(errorResult("No position available for device"));
            }

            String address = position.getAddress();
            if (address == null && geocoder != null && geocodeOnRequest) {
                position.setAddress(geocoder.getAddress(position.getLatitude(), position.getLongitude(), null));
            }

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(position)
                    .build());
        } catch (StorageException | SecurityException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    private Mono<McpSchema.CallToolResult> getDeviceList(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        Object nameValue = request.arguments().get("name");
        String nameFilter = nameValue instanceof String s && !s.isBlank()
                ? s.toLowerCase(Locale.ROOT) : null;

        int limit = limitArgument(request, 200, 1000);

        try {
            Collection<Device> devices = DeviceUtil.getAccessibleDevices(storage, userId, List.of(), List.of());
            List<Map<String, Object>> result = devices.stream()
                    .filter(device -> nameFilter == null || device.getName() != null
                            && device.getName().toLowerCase(Locale.ROOT).contains(nameFilter))
                    .map(device -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", device.getId());
                        item.put("name", device.getName());
                        item.put("uniqueId", device.getUniqueId());
                        item.put("status", device.getStatus());
                        item.put("lastUpdate", device.getLastUpdate());
                        return item;
                    })
                    .limit(limit)
                    .toList();

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of("devices", result))
                    .build());
        } catch (StorageException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    private boolean isKnownStationary(Position position) {
        return position.hasAttribute(Position.KEY_MOTION) && !position.getBoolean(Position.KEY_MOTION);
    }

    private List<Position> collapseStationaryRuns(List<Position> positions) {
        List<Position> result = new ArrayList<>();
        int i = 0;
        while (i < positions.size()) {
            if (!isKnownStationary(positions.get(i))) {
                result.add(positions.get(i));
                i++;
                continue;
            }
            int runStart = i;
            while (i < positions.size() && isKnownStationary(positions.get(i))) {
                i++;
            }
            result.add(positions.get(runStart));
            if (i - 1 > runStart) {
                result.add(positions.get(i - 1));
            }
        }
        return result;
    }

    private List<Position> downsample(List<Position> positions, int limit) {
        if (positions.size() <= limit) {
            return positions;
        }
        if (limit <= 1) {
            return List.of(positions.get(0));
        }
        List<Position> result = new ArrayList<>(limit);
        double step = (double) (positions.size() - 1) / (limit - 1);
        for (int i = 0; i < limit; i++) {
            result.add(positions.get((int) Math.round(i * step)));
        }
        return result;
    }

    private Map<String, Object> curateRoutePosition(Position position) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("latitude", position.getLatitude());
        item.put("longitude", position.getLongitude());
        item.put("speed", position.getSpeed());
        item.put("course", position.getCourse());
        item.put("fixTime", position.getFixTime());
        item.put("valid", position.getValid());
        item.put("address", position.getAddress());

        Map<String, Object> attributes = position.getAttributes();
        Object ignition = attributes.get(Position.KEY_IGNITION);
        if (ignition != null) {
            item.put("ignition", ignition);
        }
        Object batteryLevel = attributes.get(Position.KEY_BATTERY_LEVEL);
        if (batteryLevel != null) {
            item.put("batteryLevel", batteryLevel);
        }
        Object totalDistance = attributes.get(Position.KEY_TOTAL_DISTANCE);
        if (totalDistance != null) {
            item.put("totalDistance", totalDistance);
        }
        return item;
    }

    private Map<String, Object> curateTrip(TripReportItem trip) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("startTime", trip.getStartTime());
        item.put("endTime", trip.getEndTime());
        item.put("duration", trip.getDuration());
        item.put("distance", trip.getDistance());
        item.put("averageSpeed", trip.getAverageSpeed());
        item.put("maxSpeed", trip.getMaxSpeed());
        item.put("spentFuel", trip.getSpentFuel());
        item.put("startOdometer", trip.getStartOdometer());
        item.put("endOdometer", trip.getEndOdometer());
        item.put("startLat", trip.getStartLat());
        item.put("startLon", trip.getStartLon());
        item.put("endLat", trip.getEndLat());
        item.put("endLon", trip.getEndLon());
        item.put("startAddress", trip.getStartAddress());
        item.put("endAddress", trip.getEndAddress());
        if (trip.getDriverUniqueId() != null) {
            item.put("driverUniqueId", trip.getDriverUniqueId());
        }
        if (trip.getDriverName() != null) {
            item.put("driverName", trip.getDriverName());
        }
        return item;
    }

    private Mono<McpSchema.CallToolResult> getDeviceRoute(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        int limit = limitArgument(request, 200, 2000);

        Object dedupValue = request.arguments().get("dedup");
        boolean dedup = !(dedupValue instanceof Boolean dedupBoolean) || dedupBoolean;

        try {
            DeviceRange range = parseDeviceRange(request);
            permissionsService.get().checkPermission(Device.class, userId, range.deviceId());

            List<Position> positions;
            try (Stream<Position> stream = routeReportProvider.get().getObjects(
                    userId, List.of(range.deviceId()), List.of(), range.from(), range.to())) {
                positions = stream.toList();
            }

            List<Position> filtered = dedup ? collapseStationaryRuns(positions) : positions;
            List<Map<String, Object>> curated = downsample(filtered, limit).stream()
                    .map(this::curateRoutePosition)
                    .toList();

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of(
                            "positions", curated,
                            "returnedCount", curated.size(),
                            "rawCount", positions.size()))
                    .build());
        } catch (StorageException | SecurityException | IllegalArgumentException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    private Mono<McpSchema.CallToolResult> getDeviceTrips(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        int limit = limitArgument(request, 100, 1000);

        try {
            DeviceRange range = parseDeviceRange(request);
            permissionsService.get().checkPermission(Device.class, userId, range.deviceId());

            Collection<TripReportItem> trips = tripsReportProvider.get().getObjects(
                    userId, List.of(range.deviceId()), List.of(), range.from(), range.to());

            List<Map<String, Object>> curated = trips.stream()
                    .limit(limit)
                    .map(this::curateTrip)
                    .toList();

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of(
                            "trips", curated,
                            "returnedCount", curated.size(),
                            "rawCount", trips.size()))
                    .build());
        } catch (StorageException | SecurityException | IllegalArgumentException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    private Mono<McpSchema.CallToolResult> getDeviceSummary(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        Object dailyValue = request.arguments().get("daily");
        boolean daily = dailyValue instanceof Boolean dailyBoolean && dailyBoolean;
        int limit = limitArgument(request, 60, 1000);

        try {
            DeviceRange range = parseDeviceRange(request);
            permissionsService.get().checkPermission(Device.class, userId, range.deviceId());

            Collection<SummaryReportItem> summaries = summaryReportProvider.get().getObjects(
                    userId, List.of(range.deviceId()), List.of(), range.from(), range.to(), daily);

            List<Map<String, Object>> curated = summaries.stream().limit(limit).map(summary -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("startTime", summary.getStartTime());
                item.put("endTime", summary.getEndTime());
                item.put("distance", summary.getDistance());
                item.put("averageSpeed", summary.getAverageSpeed());
                item.put("maxSpeed", summary.getMaxSpeed());
                item.put("spentFuel", summary.getSpentFuel());
                item.put("startOdometer", summary.getStartOdometer());
                item.put("endOdometer", summary.getEndOdometer());
                item.put("engineHours", summary.getEngineHours());
                return item;
            }).toList();

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of(
                            "summaries", curated,
                            "returnedCount", curated.size(),
                            "rawCount", summaries.size()))
                    .build());
        } catch (StorageException | SecurityException | IllegalArgumentException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    public HttpServlet getServlet() {
        return transport;
    }

    @Override
    public void close() throws Exception {
        server.close();
    }

}
