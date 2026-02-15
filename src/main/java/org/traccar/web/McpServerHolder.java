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
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
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
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class McpServerHolder implements AutoCloseable {

    public static final String PATH = "/api/mcp";

    private final Storage storage;
    private final Provider<PermissionsService> permissionsService;
    private final Geocoder geocoder;
    private final boolean geocodeOnRequest;

    private final HttpServletStreamableServerTransportProvider transport;
    private final McpAsyncServer server;

    @Inject
    public McpServerHolder(
            ObjectMapper objectMapper, Storage storage, Provider<PermissionsService> permissionsService,
            Config config, @Nullable Geocoder geocoder) {

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
                .tools(createVersionTool(), createDevicePositionTool())
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

    private McpServerFeatures.AsyncToolSpecification createVersionTool() {

        var inputSchema = new McpSchema.JsonSchema(
                "object", Collections.emptyMap(), null, null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("traccar-version")
                .title("Returns server version name")
                .inputSchema(inputSchema)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> {
                    String version = getClass().getPackage().getImplementationVersion();
                    var result = new McpSchema.CallToolResult(version != null ? version : "Unknown", false);
                    return Mono.just(result);
                })
                .build();
    }

    private McpServerFeatures.AsyncToolSpecification createDevicePositionTool() {

        var deviceIdSchema = new McpSchema.JsonSchema(
                "number", Collections.emptyMap(), null, null, null, null);

        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("deviceId", deviceIdSchema),
                Collections.singletonList("deviceId"),
                null, null, null);

        var toolSchema = McpSchema.Tool.builder()
                .name("device-position")
                .title("Returns latest device position with address and other parameters")
                .inputSchema(inputSchema)
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
                    .isError(false)
                    .build());
        } catch (StorageException | SecurityException e) {
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
