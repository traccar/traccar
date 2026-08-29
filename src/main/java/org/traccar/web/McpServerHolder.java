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
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.RouteReportProvider;
import org.traccar.reports.SummaryReportProvider;
import org.traccar.reports.TripsReportProvider;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
                        createReportTool(
                                "device-summary",
                                "Returns distance, speed, fuel and engine hours totals for a device "
                                        + "over a time range",
                                Map.of("daily", schemaProperty(
                                        "boolean", "Return one item per day instead of one for the whole range")),
                                (userId, deviceIds, from, to, arguments) -> summaryReportProvider.get().getObjects(
                                        userId, deviceIds, List.of(), from, to,
                                        arguments.get("daily") instanceof Boolean daily && daily)),
                        createReportTool(
                                "device-trips",
                                "Returns trips for a device over a time range",
                                Map.of(),
                                (userId, deviceIds, from, to, arguments) -> tripsReportProvider.get().getObjects(
                                        userId, deviceIds, List.of(), from, to)),
                        createReportTool(
                                "device-route",
                                "Returns recorded positions for a device over a time range",
                                Map.of("limit", schemaProperty(
                                        "integer", "Maximum positions to return, evenly sampled across the range")),
                                this::getRoute))
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

    private McpServerFeatures.AsyncToolSpecification createVersionTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object", Map.of(), null, null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("traccar-version")
                .title("Returns server version name")
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

        var deviceIdSchema = new McpSchema.JsonSchema(
                "number", Map.of(), null, null, null, null);

        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("deviceId", deviceIdSchema),
                List.of("deviceId"),
                null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("device-position")
                .title("Returns latest device position with address and other parameters")
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
                        "keyword", schemaProperty("string", "Search across name, unique id, phone, model, contact"),
                        "limit", schemaProperty("integer", "Maximum number of devices to return"),
                        "offset", schemaProperty("integer", "Number of devices to skip")),
                null, null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("device-list")
                .title("Returns devices accessible to the current user")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler(this::getDeviceList)
                .build();
    }

    @FunctionalInterface
    private interface ReportFunction {
        Object getObjects(
                long userId, List<Long> deviceIds, Date from, Date to,
                Map<String, Object> arguments) throws StorageException;
    }

    private McpServerFeatures.AsyncToolSpecification createReportTool(
            String name, String title, Map<String, Object> extraProperties, ReportFunction function) {

        var properties = new LinkedHashMap<String, Object>();
        properties.put("deviceId", schemaProperty("number", "Device id, see device-list for available ids"));
        properties.put("from", schemaProperty("string", "Start of the time range, ISO-8601"));
        properties.put("to", schemaProperty("string", "End of the time range, ISO-8601"));
        properties.putAll(extraProperties);

        var inputSchema = new McpSchema.JsonSchema(
                "object", properties, List.of("deviceId", "from", "to"), null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name(name)
                .title(title)
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> getReport(context, request, function))
                .build();
    }

    private Mono<McpSchema.CallToolResult> getReport(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request, ReportFunction function) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        try {
            Object deviceIdValue = request.arguments().get("deviceId");
            if (!(deviceIdValue instanceof Number deviceIdNumber)) {
                return Mono.just(errorResult("deviceId argument is required"));
            }
            Object fromValue = request.arguments().get("from");
            Object toValue = request.arguments().get("to");
            if (!(fromValue instanceof String fromText) || !(toValue instanceof String toText)) {
                return Mono.just(errorResult("from and to arguments are required"));
            }

            long deviceId = deviceIdNumber.longValue();
            Date from = DateUtil.parseDate(fromText);
            Date to = DateUtil.parseDate(toText);

            permissionsService.get().checkRestriction(userId, UserRestrictions::getDisableReports);
            permissionsService.get().checkPermission(Device.class, userId, deviceId);

            Object items = function.getObjects(
                    userId, List.of(deviceId), from, to, request.arguments());

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of("items", items))
                    .build());
        } catch (DateTimeParseException e) {
            return Mono.just(errorResult("Invalid from or to value, expected ISO-8601"));
        } catch (StorageException | SecurityException e) {
            return Mono.just(errorResult(e.getMessage()));
        }
    }

    private List<Position> getRoute(
            long userId, List<Long> deviceIds, Date from, Date to,
            Map<String, Object> arguments) throws StorageException {

        List<Position> positions;
        try (Stream<Position> stream = routeReportProvider.get().getObjects(
                userId, deviceIds, List.of(), from, to)) {
            positions = stream.toList();
        }

        int limit = arguments.get("limit") instanceof Number number ? number.intValue() : 0;
        if (limit <= 0 || positions.size() <= limit) {
            return positions;
        }
        if (limit == 1) {
            return List.of(positions.get(0));
        }

        List<Position> result = new ArrayList<>(limit);
        double step = (double) (positions.size() - 1) / (limit - 1);
        for (int i = 0; i < limit; i++) {
            result.add(positions.get((int) Math.round(i * step)));
        }
        return result;
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
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

    private int intArgument(McpSchema.CallToolRequest request, String name) {
        return request.arguments().get(name) instanceof Number number ? number.intValue() : 0;
    }

    private Mono<McpSchema.CallToolResult> getDeviceList(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request) {

        Long userId = (Long) context.transportContext().get(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId == null) {
            return Mono.just(errorResult("User context is missing"));
        }

        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Permission(User.class, userId, Device.class));

        Object keyword = request.arguments().get("keyword");
        if (keyword instanceof String text && !text.isEmpty()) {
            conditions.add(new Condition.Contains(
                    List.of("name", "uniqueId", "phone", "model", "contact"), text));
        }

        var order = new Order(
                "name", false, intArgument(request, "limit"), intArgument(request, "offset"));

        try {
            List<Device> devices = storage.getObjects(Device.class, new Request(
                    new Columns.All(), Condition.merge(conditions), order));

            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(Map.of("devices", devices))
                    .build());
        } catch (StorageException e) {
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
