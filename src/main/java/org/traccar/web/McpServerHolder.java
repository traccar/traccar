/*
 * Copyright 2025 - 2026 Anton Tananaev (anton@traccar.org)
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
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Singleton
public class McpServerHolder implements AutoCloseable {

    public static final String PATH = "/api/mcp";

    private static final McpSchema.ToolAnnotations READ_ONLY_ANNOTATIONS = new McpSchema.ToolAnnotations(
            null, true, false, true, false, null);

    private final McpApiInvoker apiInvoker;

    private final HttpServletStreamableServerTransportProvider transport;
    private final McpAsyncServer server;

    @Inject
    public McpServerHolder(ObjectMapper objectMapper, McpToolRegistry toolRegistry, McpApiInvoker apiInvoker) {

        this.apiInvoker = apiInvoker;

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
                .tools(toolRegistry.getTools().stream().map(this::createApiTool).toList())
                .build();
    }

    private McpTransportContext extractTransportContext(HttpServletRequest request) {
        var contextData = new HashMap<String, Object>();
        Object userId = request.getAttribute(McpAuthFilter.ATTRIBUTE_USER_ID);
        if (userId != null) {
            contextData.put(McpAuthFilter.ATTRIBUTE_USER_ID, userId);
        }
        Object authorization = request.getAttribute(McpAuthFilter.ATTRIBUTE_AUTHORIZATION);
        if (authorization != null) {
            contextData.put(McpAuthFilter.ATTRIBUTE_AUTHORIZATION, authorization);
        }
        if (contextData.isEmpty()) {
            return McpTransportContext.EMPTY;
        }
        return McpTransportContext.create(contextData);
    }

    private McpServerFeatures.AsyncToolSpecification createApiTool(McpToolRegistry.McpApiTool tool) {

        var toolSchema = McpSchema.Tool.builder()
                .name(tool.name())
                .title(tool.title())
                .inputSchema(tool.inputSchema())
                .annotations(READ_ONLY_ANNOTATIONS)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> {
                    try {
                        String authorization =
                                (String) context.transportContext().get(McpAuthFilter.ATTRIBUTE_AUTHORIZATION);
                        String body = apiInvoker.get(tool.path(), request.arguments(), authorization);
                        return Mono.just(McpSchema.CallToolResult.builder().addTextContent(body).build());
                    } catch (RuntimeException e) {
                        return Mono.just(McpSchema.CallToolResult.builder()
                                .addTextContent(e.getMessage())
                                .isError(true)
                                .build());
                    }
                })
                .build();
    }

    public HttpServlet getServlet() {
        return transport;
    }

    @Override
    public void close() throws Exception {
        server.close();
    }

}
