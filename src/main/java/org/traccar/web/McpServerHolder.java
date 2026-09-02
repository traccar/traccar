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
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class McpServerHolder implements AutoCloseable {

    public static final String PATH = "/api/mcp";

    private static final String ATTRIBUTE_REQUEST = "request";
    private static final Type REQUEST_REF_TYPE = new GenericType<Ref<HttpServletRequest>>() { }.getType();

    private static final McpSchema.ToolAnnotations READ_ONLY_ANNOTATIONS = new McpSchema.ToolAnnotations(
            null, true, false, true, false, null);

    private final ObjectMapper objectMapper;
    private final Storage storage;
    private final Provider<PermissionsService> permissionsService;
    private final Geocoder geocoder;
    private final boolean geocodeOnRequest;

    private final HttpServletStreamableServerTransportProvider transport;
    private final McpAsyncServer server;

    private Supplier<ApplicationHandler> apiHandler;

    @Inject
    public McpServerHolder(
            ObjectMapper objectMapper, Storage storage, Provider<PermissionsService> permissionsService,
            Config config, @Nullable Geocoder geocoder) {

        this.objectMapper = objectMapper;
        this.storage = storage;
        this.permissionsService = permissionsService;
        this.geocoder = geocoder;
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
                .tools(createVersionTool(), createDevicePositionTool(), createSummaryReportTool())
                .build();
    }

    public void setApiHandler(Supplier<ApplicationHandler> apiHandler) {
        this.apiHandler = apiHandler;
    }

    private McpTransportContext extractTransportContext(HttpServletRequest request) {
        var contextData = new HashMap<String, Object>();
        Object userId = request.getAttribute(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId != null) {
            contextData.put(McpAuthFilter.ATTRIBUTE_USER_ID, userId);
        }
        contextData.put(ATTRIBUTE_REQUEST, request);
        if (contextData.isEmpty()) {
            return McpTransportContext.EMPTY;
        }
        return McpTransportContext.create(contextData);
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


    private McpServerFeatures.AsyncToolSpecification createSummaryReportTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "deviceId", Map.of("type", "array", "items", Map.of("type", "integer")),
                        "from", Map.of("type", "string"),
                        "to", Map.of("type", "string")),
                List.of("deviceId", "from", "to"),
                null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("reports-summary")
                .title("Returns summary report for devices over a time range")
                .inputSchema(inputSchema)
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> callApi(context, request, "reports/summary"))
                .build();
    }

    private Mono<McpSchema.CallToolResult> callApi(
            McpAsyncServerExchange context, McpSchema.CallToolRequest request, String path) {

        var servletRequest = (HttpServletRequest) context.transportContext().get(ATTRIBUTE_REQUEST);
        if (servletRequest == null) {
            return Mono.just(errorResult("Request context is missing"));
        }

        var query = new StringBuilder();
        request.arguments().forEach((key, value) -> {
            var values = value instanceof Collection<?> collection ? collection : List.of(value);
            for (Object item : values) {
                query.append(query.isEmpty() ? '?' : '&').append(key).append('=')
                        .append(URLEncoder.encode(String.valueOf(item), StandardCharsets.UTF_8));
            }
        });

        URI baseUri = URI.create("http://localhost/api/");
        var apiRequest = new ContainerRequest(
                baseUri, baseUri.resolve(path + query), "GET", null, new MapPropertiesDelegate());
        apiRequest.header("Authorization", servletRequest.getHeader("Authorization"));
        apiRequest.header("Accept", MediaType.APPLICATION_JSON);
        apiRequest.setRequestScopedInitializer(injectionManager ->
                injectionManager.<Ref<HttpServletRequest>>getInstance(REQUEST_REF_TYPE).set(servletRequest));

        try {
            var output = new ByteArrayOutputStream();
            ContainerResponse response = apiHandler.get().apply(apiRequest, output).get();
            String body = output.toString(StandardCharsets.UTF_8);
            if (response.getStatus() >= 400) {
                return Mono.just(errorResult(
                        "HTTP " + response.getStatus() + " " + body.lines().findFirst().orElse("")));
            }
            Object result = body.isEmpty() ? Map.of() : objectMapper.readValue(body, Object.class);
            return Mono.just(McpSchema.CallToolResult.builder()
                    .structuredContent(result instanceof List<?> ? Map.of("items", result) : result)
                    .build());
        } catch (Exception e) {
            return Mono.just(errorResult(e.toString()));
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
