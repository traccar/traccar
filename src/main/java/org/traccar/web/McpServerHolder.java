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
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServlet;

@Singleton
public class McpServerHolder implements AutoCloseable {

    public static final String PATH = "/api/mcp";

    private final HttpServletStatelessServerTransport transport;
    private final McpStatelessSyncServer server;

    @Inject
    public McpServerHolder(ObjectMapper objectMapper) {

        transport = HttpServletStatelessServerTransport.builder()
                .messageEndpoint(PATH)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .build();

        var capabilities = McpSchema.ServerCapabilities.builder()
                .tools(true)
                .resources(false, true)
                .prompts(true)
                .build();

        server = McpServer.sync(transport)
                .serverInfo("traccar-mcp", "1.0.0")
                .capabilities(capabilities)
                .tools(createVersionTool())
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification createVersionTool() {

        var toolSchema = McpSchema.Tool.builder()
                .name("traccar-version")
                .title("Returns server version name")
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(toolSchema)
                .callHandler((context, request) -> {
                    String version = getClass().getPackage().getImplementationVersion();
                    return new McpSchema.CallToolResult(version != null ? version : "Unknown", false);
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
